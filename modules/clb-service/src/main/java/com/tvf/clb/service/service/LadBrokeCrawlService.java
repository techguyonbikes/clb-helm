package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.entity.*;
import com.tvf.clb.base.model.EntrantRawData;
import com.tvf.clb.base.model.MeetingRawData;
import com.tvf.clb.base.model.RaceRawData;
import com.tvf.clb.base.model.VenueRawData;
import com.tvf.clb.base.utils.ApiUtils;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.service.repository.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ClbService(componentType = AppConstant.LAD_BROKE)
@Slf4j
public class LadBrokeCrawlService implements ICrawlService {

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private RaceRepository raceRepository;

    @Autowired
    private EntrantRepository entrantRepository;

    @Autowired
    private CrawUtils crawUtils;

    @Autowired
    private EntrantSiteRepository entrantSiteRepository;

    @Autowired
    private ServiceLookup serviceLookup;

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {
        return Mono.fromSupplier(() -> {
            LadBrokedItMeetingDto rawData = null;
            try {
                String url = AppConstant.LAD_BROKES_IT_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
                Response response = ApiUtils.get(url);
                ResponseBody body = response.body();
                Gson gson = new GsonBuilder().setDateFormat(AppConstant.DATE_TIME_FORMAT_LONG).create();
                if (body != null) {
                    rawData = gson.fromJson(body.string(), LadBrokedItMeetingDto.class);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return getAllAusMeeting(rawData, date);
        }).flatMapMany(Flux::fromIterable);
    }

    private List<MeetingDto> getAllAusMeeting(LadBrokedItMeetingDto ladBrokedItMeetingDto,LocalDate date) {
        List<VenueRawData> ausVenues = ladBrokedItMeetingDto.getVenues().values().stream().filter(v -> v.getCountry().equals(AppConstant.AUS)).collect(Collectors.toList());
        List<String> venuesId = ausVenues.stream().map(VenueRawData::getId).collect(Collectors.toList());
        List<MeetingRawData> meetings = new ArrayList<>(ladBrokedItMeetingDto.getMeetings().values());
        List<MeetingRawData> ausMeetings = meetings.stream().filter(m -> StringUtils.hasText(m.getCountry()) && venuesId.contains(m.getVenueId())).collect(Collectors.toList());
        List<String> raceIds = ausMeetings.stream().map(MeetingRawData::getRaceIds).flatMap(List::stream)
                .collect(Collectors.toList());
        List<RaceRawData> ausRace = ladBrokedItMeetingDto.getRaces()
                .values().stream().filter(r -> raceIds.contains(r.getId())).collect(Collectors.toList());
        List<MeetingDto> meetingDtoList = new ArrayList<>();
        for (MeetingRawData localMeeting : ausMeetings) {
            List<RaceRawData> localRace = ausRace.stream().filter(r -> localMeeting.getRaceIds().contains(r.getId())).collect(Collectors.toList());
            MeetingDto meetingDto = MeetingMapper.toMeetingDto(localMeeting, localRace);
            meetingDtoList.add(meetingDto);
        }
        saveMeeting(ausMeetings);
        List<RaceDto> raceDtoList = meetingDtoList.stream().map(MeetingDto::getRaces).flatMap(List::stream).collect(Collectors.toList());
        saveRace(raceDtoList);
        getEntrantRaceByIds(ausRace
                .stream()
                .map(RaceRawData::getId).collect(Collectors.toList()), date);
        return meetingDtoList;
    }

    private void getEntrantRaceByIds(List<String> raceIds, LocalDate date) {
       Flux.fromIterable(raceIds)
//                .parallel() // create a parallel flux
//                .runOn(Schedulers.parallel()) // specify which scheduler to use for the parallel execution
                .flatMap(raceId -> getEntrantByRaceId(raceId).subscribeOn(Schedulers.parallel())) // call the getRaceById method for each raceId
//                .sequential() // convert back to a sequential flux
                .collectList()
                .subscribe(x -> getMeetingFromAllSite(date).subscribe());
    }

    private Flux<EntrantDto> getEntrantByRaceId(String raceId) {
        try {
            LadBrokedItRaceDto raceDto = getLadBrokedItRaceDto(raceId);
            JsonObject results = raceDto.getResults();
            Map<String, Integer> positions = new HashMap<>();
            String statusRace = null;
            if (results != null) {
                positions = results.keySet().stream().collect(Collectors.toMap(Function.identity(), key -> results.getAsJsonObject(key).get("position").getAsInt()));
                statusRace = String.valueOf(Race.Status.F);
            } else {
                positions.put("position", 0);
                statusRace = String.valueOf(Race.Status.O);
            }
//            String distance = raceDto.getRaces().getAsJsonObject(raceId).getAsJsonObject("additional_info").get("distance").getAsString();
//            raceRepository.getRaceByRaceSiteId(raceId).subscribe(x -> log.info(x.getName()));
//            raceRepository.setUpdateRaceByRaceId(raceId, distance == null ? 0 : Integer.parseInt(distance), statusRace).subscribe();
            HashMap<String, ArrayList<Float>> allEntrantPrices = raceDto.getPriceFluctuations();
            List<EntrantRawData> allEntrant = getListEntrant(raceDto, allEntrantPrices, raceId, positions);
            saveEntrant(allEntrant);
            return Flux.fromIterable(allEntrant)
                    .flatMap(r -> {
                        List<Float> entrantPrices = CollectionUtils.isEmpty(allEntrantPrices) ? new ArrayList<>() : allEntrantPrices.get(r.getId());
                        EntrantDto entrantDto = EntrantMapper.toEntrantDto(r, entrantPrices);
                        return Mono.just(entrantDto);
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveMeeting(List<MeetingRawData> meetingRawData) {
        List<Meeting> newMeetings = meetingRawData.stream().map(MeetingMapper::toMeetingEntity).collect(Collectors.toList());
        List<String> meetingNameList = newMeetings.stream().map(Meeting::getName).collect(Collectors.toList());
        List<String> raceTypeList = newMeetings.stream().map(Meeting::getRaceType).collect(Collectors.toList());
        List<Instant> dates = newMeetings.stream().map(Meeting::getAdvertisedDate).collect(Collectors.toList());
        Flux<Meeting> existedMeeting = meetingRepository
                .findAllByNameInAndRaceTypeInAndAdvertisedDateIn(meetingNameList, raceTypeList, dates);
        existedMeeting
                .collectList()
                .subscribe(existed ->
                        {
                            newMeetings.addAll(existed);
                            List<Meeting> meetingNeedUpdateOrInsert = newMeetings.stream().distinct().peek(m ->
                            {
                                if (m.getId() == null) {
                                    existed.stream()
                                            .filter(x -> x.getName().equals(m.getName()) && x.getAdvertisedDate().equals(m.getAdvertisedDate()))
                                            .findFirst()
                                            .ifPresent(meeting -> m.setId(meeting.getId()));
                                }
                            }).filter(e -> !existed.contains(e)).collect(Collectors.toList());
                            log.info("Meeting need to be update or insert " + meetingNeedUpdateOrInsert.size());
                            Flux.fromIterable(meetingNeedUpdateOrInsert)
                                    .parallel() // parallelize the processing
                                    .runOn(Schedulers.parallel()) // specify the parallel scheduler
                                    .flatMap(meeting -> meetingRepository.save(meeting))
                                    .sequential() // switch back to sequential processing
                                    .collectList()
                                    .subscribe(savedMeetings -> {
                                        crawUtils.saveMeetingSite(savedMeetings, 1);
                                        log.info("All meetings processed successfully");
                                    });
                        }
                );
    }

    public void saveRace(List<RaceDto> raceDtoList) {
        List<Race> newRaces = raceDtoList.stream().map(MeetingMapper::toRaceEntity).filter(x -> x.getNumber() != null).collect(Collectors.toList());
        List<String> raceNames = newRaces.stream().map(Race::getName).collect(Collectors.toList());
        List<Integer> raceNumbers = newRaces.stream().map(Race::getNumber).collect(Collectors.toList());
        List<Instant> dates = newRaces.stream().map(Race::getAdvertisedStart).collect(Collectors.toList());
        Flux<Race> existedRaces = raceRepository
                .findAllByNameInAndNumberInAndAdvertisedStartIn(raceNames, raceNumbers, dates);
        existedRaces
                .collectList()
                .subscribe(existed ->
                        {
                            newRaces.addAll(existed);
                            List<Race> raceNeedUpdateOrInsert = newRaces.stream().distinct().peek(e ->
                            {
                                if (e.getId() == null) {
                                    existed.stream()
                                            .filter(x -> x.getName().equals(e.getName())
                                                    && x.getNumber().equals(e.getNumber()))
                                            .findFirst()
                                            .ifPresent(entrant -> e.setId(entrant.getId()));
                                }
                            }).filter(e -> !existed.contains(e)).collect(Collectors.toList());
                            log.info("Race need to be update is " + raceNeedUpdateOrInsert.size());
                            Flux.fromIterable(raceNeedUpdateOrInsert)
                                    .parallel() // parallelize the processing
                                    .runOn(Schedulers.parallel()) // specify the parallel scheduler
                                    .flatMap(race -> raceRepository.save(race))
                                    .sequential() // switch back to sequential processing
                                    .collectList()
                                    .subscribe(savedRace -> {
                                        crawUtils.saveRaceSite(savedRace, 1);
                                        log.info("All races processed successfully");
                                    });
                        }
                );
    }

    public void saveEntrant(List<EntrantRawData> entrantRawData) {
        List<Entrant> newEntrants = entrantRawData.stream().map(MeetingMapper::toEntrantEntity).collect(Collectors.toList());
        Flux<Long> entrantIds = entrantSiteRepository
                .findAllByEntrantSiteIdIn(newEntrants.stream()
                        .map(Entrant::getEntrantId).collect(Collectors.toList()))
                .map(EntrantSite::getGeneralEntrantId);
        entrantIds.collectList().subscribe(r -> entrantRepository.findAllByIdIn(r)
                .collectList()
                .subscribe(existed ->
                        {
                            newEntrants.addAll(existed);
                            List<Entrant> entrantNeedUpdateOrInsert = newEntrants.stream().distinct().peek(e ->
                            {
                                if (e.getId() == null) {
                                    existed.stream()
                                            .filter(x -> x.getName().equals(e.getName())
                                            && x.getNumber().equals(e.getNumber())
                                            && x.getBarrier().equals(e.getBarrier()))
                                            .findFirst()
                                            .ifPresent(entrant -> e.setId(entrant.getId()));
                                }
                            }).filter(e -> !existed.contains(e)).collect(Collectors.toList());
                            log.info("Entrant need to be update is " + entrantNeedUpdateOrInsert.size());
                            entrantRepository.saveAll(entrantNeedUpdateOrInsert).collectList().subscribe(entrants -> crawUtils.saveEntrantSite(entrants, 1));
                        }
                ));
    }

    private List<EntrantRawData> getListEntrant(LadBrokedItRaceDto raceDto, Map<String, ArrayList<Float>> allEntrantPrices, String raceId, Map<String, Integer> positions) {
        return raceDto.getEntrants().values().stream().filter(r -> r.getFormSummary() != null && r.getId() != null).map(r -> {
            List<Float> entrantPrices = allEntrantPrices == null ? new ArrayList<>() : allEntrantPrices.get(r.getId());
            Integer entrantPosition = positions.get(r.getId()) == null ? 0 : positions.get(r.getId());
            EntrantRawData entrantRawData = EntrantMapper.mapPrices(r, entrantPrices, entrantPosition);
            entrantRawData.setRaceId(raceId);
            return entrantRawData;
        }).collect(Collectors.toList());
    }

    private LadBrokedItRaceDto getLadBrokedItRaceDto(String raceId) throws IOException {
        String url = AppConstant.LAD_BROKES_IT_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
        Response response = ApiUtils.get(url);
        JsonObject jsonObject = JsonParser.parseString(response.body().string()).getAsJsonObject();
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(jsonObject.get("data"), LadBrokedItRaceDto.class);
    }

    //after every thing is implement for first time then we call all site. we need to save all common data first
    private Flux<MeetingDto> getMeetingFromAllSite(LocalDate date) {
        List<ICrawlService> crawlServices = new ArrayList<>();
        for (String site: AppConstant.SITE_LIST) {
            crawlServices.add(serviceLookup.forBean(ICrawlService.class, site));
        }
        return Flux.fromIterable(crawlServices)
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(x -> x.getTodayMeetings(date))
                .sequential();
    }
}
