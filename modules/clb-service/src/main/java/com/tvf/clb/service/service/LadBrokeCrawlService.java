package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.exception.ApiRequestFailedException;
import com.tvf.clb.base.model.*;
import com.tvf.clb.base.utils.ApiUtils;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.service.repository.EntrantRepository;
import com.tvf.clb.service.repository.MeetingRepository;
import com.tvf.clb.service.repository.RaceRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
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
    private ServiceLookup serviceLookup;

    @Autowired
    private EntrantRedisService entrantRedisService;

    @Autowired
    private ReactiveRedisTemplate<String, Long> raceNameAndIdTemplate;

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
                throw new ApiRequestFailedException("API request failed: " + e.getMessage(), e);
            }
            return getAllAusMeeting(rawData, date);
        }).flatMapMany(Flux::fromIterable);
    }

    @Override
    public Map<Integer, CrawlEntrantData> getEntrantByRaceUUID(String raceId) {
        try {
            LadBrokedItRaceDto raceDto = getLadBrokedItRaceDto(raceId);
            JsonObject results = raceDto.getResults();
            Map<String, Integer> positions = new HashMap<>();
            if (results != null) {
                positions = results.keySet().stream().collect(Collectors.toMap(Function.identity(), key -> results.getAsJsonObject(key).get(AppConstant.POSITION).getAsInt()));
            } else {
                positions.put(AppConstant.POSITION, 0);
            }
            HashMap<String, ArrayList<Float>> allEntrantPrices = raceDto.getPriceFluctuations();
            List<EntrantRawData> allEntrant = getListEntrant(raceDto, allEntrantPrices, raceId, positions);
            Map<Integer, CrawlEntrantData> result = new HashMap<>();
            allEntrant.forEach(x -> {
                List<Float> entrantPrice = allEntrantPrices.get(x.getId()) == null ? new ArrayList<>()
                        : new ArrayList<>(allEntrantPrices.get(x.getId()));
                Map<Integer, List<Float>> priceFluctuations = new HashMap<>();
                priceFluctuations.put(AppConstant.LAD_BROKE_SITE_ID, entrantPrice);
                result.put(x.getNumber(), new CrawlEntrantData(x.getPosition(), AppConstant.LAD_BROKE_SITE_ID, priceFluctuations));
            });
            return result;
        } catch (IOException e) {
            throw new ApiRequestFailedException("API request failed: " + e.getMessage(), e);
        }
    }

    private List<MeetingDto> getAllAusMeeting(LadBrokedItMeetingDto ladBrokedItMeetingDto, LocalDate date) {
        List<VenueRawData> ausVenues = ladBrokedItMeetingDto.getVenues().values().stream().filter(v -> AppConstant.VALID_COUNTRY_CODE.contains(v.getCountry())).collect(Collectors.toList());
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

        List<RaceDto> raceDtoList = meetingDtoList.stream().map(MeetingDto::getRaces).flatMap(List::stream).collect(Collectors.toList());
        saveMeetingAndRace(ausMeetings, raceDtoList, date);
        return meetingDtoList;
    }


    private Flux<Entrant> getEntrantByRaceId(String raceId, Long generalRaceId) {
        try {
            LadBrokedItRaceDto raceDto = getLadBrokedItRaceDto(raceId);
            JsonObject results = raceDto.getResults();
            Map<String, Integer> positions = new HashMap<>();
            String statusRace = null;
            if (results != null) {
                positions = results.keySet().stream().collect(Collectors.toMap(Function.identity(), key -> results.getAsJsonObject(key).get(AppConstant.POSITION).getAsInt()));
                statusRace = String.valueOf(Race.Status.F);
            } else {
                positions.put(AppConstant.POSITION, 0);
                statusRace = String.valueOf(Race.Status.O);
            }
            //got null every time?
            String distance = raceDto.getRaces().getAsJsonObject(raceId).getAsJsonObject(AppConstant.ADDITIONAL_INFO).get(AppConstant.DISTANCE).getAsString();
            raceRepository.setUpdateRaceById(generalRaceId, distance == null ? 0 : Integer.parseInt(distance), statusRace).subscribe();
            HashMap<String, ArrayList<Float>> allEntrantPrices = raceDto.getPriceFluctuations();
            List<EntrantRawData> allEntrant = getListEntrant(raceDto, allEntrantPrices, raceId, positions);
            return saveEntrant(allEntrant, generalRaceId);
        } catch (IOException e) {
            throw new ApiRequestFailedException("API request failed: " + e.getMessage(), e);
        }
    }

    private void saveMeetingAndRace(List<MeetingRawData> meetingRawData, List<RaceDto> raceDtoList, LocalDate date) {
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
                            List<Meeting> meetingNeedUpdateOrInsert = newMeetings.stream().distinct().map(m ->
                            {
                                if (m.getId() == null) {
                                    existed.stream()
                                            .filter(x -> x.getName().equals(m.getName()) && x.getAdvertisedDate().equals(m.getAdvertisedDate()))
                                            .findFirst()
                                            .ifPresent(meeting -> m.setId(meeting.getId()));
                                }
                                return m;
                            }).filter(e -> !existed.contains(e)).collect(Collectors.toList());
                            log.info("Meeting need to be update or insert " + meetingNeedUpdateOrInsert.size());
                            Flux.fromIterable(meetingNeedUpdateOrInsert)
                                    .parallel() // parallelize the processing
                                    .runOn(Schedulers.parallel()) // specify the parallel scheduler
                                    .flatMap(meeting -> meetingRepository.save(meeting))
                                    .sequential() // switch back to sequential processing
                                    .collectList()
                                    .subscribe(savedMeetings -> {
                                        saveRace(raceDtoList, savedMeetings, date);
                                        crawUtils.saveMeetingSite(savedMeetings, AppConstant.LAD_BROKE_SITE_ID);
                                        log.info("All meetings processed successfully");
                                    });
                        }
                );
    }

    public void saveRace(List<RaceDto> raceDtoList, List<Meeting> savedMeeting, LocalDate date) {
        Map<String, Meeting> meetingUUIDMap = savedMeeting.stream().collect(Collectors.toMap(Meeting::getMeetingId, Function.identity()));
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
                            List<Race> raceNeedUpdateOrInsert = newRaces.stream().distinct().map(e ->
                            {
                                Meeting meeting = meetingUUIDMap.getOrDefault(e.getMeetingUUID(), null);
                                if (meeting != null) {
                                    e.setMeetingId(meeting.getId());
                                }
                                if (e.getId() == null) {
                                    existed.stream()
                                            .filter(x -> x.getName().equals(e.getName())
                                                    && x.getNumber().equals(e.getNumber()))
                                            .findFirst()
                                            .ifPresent(entrant -> e.setId(entrant.getId()));
                                }
                                return e;
                            }).filter(e -> !existed.contains(e)).collect(Collectors.toList());
                            log.info("Race need to be update is " + raceNeedUpdateOrInsert.size());
                            Flux.fromIterable(raceNeedUpdateOrInsert)
                                    .parallel() // parallelize the processing
                                    .runOn(Schedulers.parallel()) // specify the parallel scheduler
                                    .flatMap(race -> raceRepository.save(race))
                                    .sequential() // switch back to sequential processing
                                    .collectList()
                                    .subscribe(savedRace -> {
                                        Flux.fromIterable(savedRace)
                                                .parallel()
                                                .runOn(Schedulers.parallel())
                                                .flatMap(race -> getEntrantByRaceId(race.getRaceId(), race.getId()))
                                                .sequential()
                                                .collectList()
                                                .subscribe(x -> getMeetingFromAllSite(date).subscribe());
                                        savedRace.forEach(race -> {
                                            Meeting raceMeeting = meetingUUIDMap.get(race.getMeetingUUID());
                                            String key = String.format("%s - %s - %s - %s", raceMeeting.getName(), race.getNumber(), raceMeeting.getRaceType(), date);
                                            raceNameAndIdTemplate.opsForValue().set(key, race.getId()).subscribe();
                                        });
                                        crawUtils.saveRaceSite(savedRace, AppConstant.LAD_BROKE_SITE_ID);
                                        log.info("All races processed successfully");
                                    });
                        }
                );
    }

    public Flux<Entrant> saveEntrant(List<EntrantRawData> entrantRawData, Long raceId) {
        List<Entrant> newEntrants = entrantRawData.stream().map(m -> MeetingMapper.toEntrantEntity(m, AppConstant.LAD_BROKE_SITE_ID)).collect(Collectors.toList());
        List<String> entrantNames = newEntrants.stream().map(Entrant::getName).collect(Collectors.toList());
        List<Integer> entrantNumbers = newEntrants.stream().map(Entrant::getNumber).collect(Collectors.toList());
        List<Integer> entrantBarriers = newEntrants.stream().map(Entrant::getBarrier).collect(Collectors.toList());
        Flux<Entrant> existedEntrants = entrantRepository
                .findAllByNameInAndNumberInAndBarrierIn(entrantNames, entrantNumbers, entrantBarriers);
        return existedEntrants
                .collectList()
                .flatMapMany(existed ->
                        {
                            newEntrants.addAll(existed);
                            List<Entrant> entrantNeedUpdateOrInsert = newEntrants.stream().distinct().map(e ->
                            {
                                e.setRaceId(raceId);
                                if (e.getId() == null) {
                                    existed.stream()
                                            .filter(x -> x.getName().equals(e.getName())
                                                    && x.getNumber().equals(e.getNumber())
                                                    && x.getBarrier().equals(e.getBarrier()))
                                            .findFirst()
                                            .ifPresent(entrant -> e.setId(entrant.getId()));
                                }
                                return e;
                            }).filter(e -> !existed.contains(e)).collect(Collectors.toList());
                            log.info("Entrant need to be update is {}", entrantNeedUpdateOrInsert.size());
                            return entrantRepository.saveAll(entrantNeedUpdateOrInsert)
                                    .collectList()
                                    .flatMapMany(saved -> {
                                        entrantRedisService
                                                .saveRace(raceId, saved.stream()
                                                        .map(x -> EntrantMapper.toEntrantResponseDto(x, AppConstant.LAD_BROKE_SITE_ID))
                                                        .collect(Collectors.toList())).subscribe();
                                        log.info("{} entrants save into redis and database", saved.size());
                                        return Flux.fromIterable(saved);
                                    });
                        }
                );
    }

    private List<EntrantRawData> getListEntrant(LadBrokedItRaceDto raceDto, Map<String, ArrayList<Float>> allEntrantPrices, String raceId, Map<String, Integer> positions) {
        return raceDto.getMarkets().values().stream()
                .filter(m -> m.getName().equals(AppConstant.MARKETS_NAME))
                .findFirst()
                .map(LadbrokesMarketsRawData::getRace_id)
                .orElse(null)
                .stream()
                .map(x -> raceDto.getEntrants().get(x))
                .filter(r -> r.getFormSummary() != null && r.getId() != null)
                .map(r -> {
                    List<Float> entrantPrices = allEntrantPrices == null ? new ArrayList<>() : allEntrantPrices.getOrDefault(r.getId(), new ArrayList<>());
                    Integer entrantPosition = positions.getOrDefault(r.getId(), 0);
                    EntrantRawData entrantRawData = EntrantMapper.mapPrices(r, entrantPrices, entrantPosition);
                    entrantRawData.setRaceId(raceId);
                    return entrantRawData;
                })
                .collect(Collectors.toList());
    }

    private LadBrokedItRaceDto getLadBrokedItRaceDto(String raceId) throws IOException {
        String url = AppConstant.LAD_BROKES_IT_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
        Response response = ApiUtils.get(url);
        JsonObject jsonObject = JsonParser.parseString(response.body().string()).getAsJsonObject();
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(jsonObject.get("data"), LadBrokedItRaceDto.class);
    }

    //after every thing is implement for first time then we call all site. we need to save all common data first
    @SneakyThrows
    private Flux<MeetingDto> getMeetingFromAllSite(LocalDate date) {
        List<ICrawlService> crawlServices = new ArrayList<>();
        for (String site : AppConstant.SITE_LIST) {
            crawlServices.add(serviceLookup.forBean(ICrawlService.class, site));
        }
        return Flux.fromIterable(crawlServices)
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(x -> x.getTodayMeetings(date))
                .sequential();
    }
}
