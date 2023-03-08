package com.tvf.clb.service.service;

import com.google.gson.*;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.entity.*;
import com.tvf.clb.base.model.*;
import com.tvf.clb.base.utils.ApiUtils;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.service.repository.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tvf.clb.base.utils.AppConstant.TIME_VALIDATE_START;


@Service
@Slf4j
public class CrawlService {

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private RaceRepository raceRepository;

    @Autowired
    private EntrantRepository entrantRepository;


    public Mono<List<MeetingDto>> getTodayMeetings(LocalDate date) {
        return Mono.fromSupplier(() -> {
            LadBrokedItMeetingDto rawData = null;
            try {
                String url = AppConstant.LAD_BROKES_IT_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
                Response response = ApiUtils.get(url);
                ResponseBody body = response.body();
                Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").create();
                if (body != null) {
                    rawData = gson.fromJson(body.string(), LadBrokedItMeetingDto.class);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return getAllAusMeeting(rawData);
        });
    }

    private List<MeetingDto> getAllAusMeeting(LadBrokedItMeetingDto ladBrokedItMeetingDto) {
        List<VenueRawData> ausVenues = ladBrokedItMeetingDto.getVenues().values().stream().filter(v -> v.getCountry().equals(AppConstant.AUS)).collect(Collectors.toList());
        List<String> venuesId = ausVenues.stream().map(VenueRawData::getId).collect(Collectors.toList());
        List<MeetingRawData> meetings = new ArrayList<>(ladBrokedItMeetingDto.getMeetings().values());
        List<MeetingRawData> ausMeetings = meetings.stream().filter(m -> StringUtils.hasText(m.getCountry()) && venuesId.contains(m.getVenueId())).collect(Collectors.toList());
        List<String> raceIds = ausMeetings.stream().map(MeetingRawData::getRaceIds).flatMap(List::stream)
                .collect(Collectors.toList());
        List<RaceRawData> ausRace = ladBrokedItMeetingDto.getRaces()
                .values().stream().filter(r -> raceIds.contains(r.getId())).collect(Collectors.toList());
        List<MeetingDto> meetingDtoList = new ArrayList<>();
        List<RaceRawData> newRacesList = new ArrayList<>();
        for (RaceRawData raceRawData : ausRace) {
            if (Instant.parse(raceRawData.getActualStart()).isAfter(Instant.now().minusSeconds(TIME_VALIDATE_START))) {
                newRacesList.add(raceRawData);
            }
        }
        for (MeetingRawData localMeeting : ausMeetings) {
            List<RaceRawData> localRace = newRacesList.stream().filter(r -> localMeeting.getRaceIds().contains(r.getId()))
                    .collect(Collectors.toList());

            MeetingDto meetingDto = MeetingMapper.toMeetingDto(localMeeting, localRace);
            meetingDtoList.add(meetingDto);
        }
        saveMeeting(ausMeetings);
        List<RaceDto> raceDtoList = meetingDtoList.stream().map(MeetingDto::getRaces).flatMap(List::stream).map(r->{
            LadBrokedItRaceDto raceDto = null;
            try {
                raceDto = getLadBrokedItRaceDto(r.getId());
                String distance = raceDto.getRaces().getAsJsonObject(r.getId()).getAsJsonObject("additional_info").get("distance").getAsString();
                if (distance == null) {
                    r.setDistance(0);
                } else {
                    r.setDistance(Integer.valueOf(distance));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
           return r;
        }).collect(Collectors.toList());
        saveRace(raceDtoList);
        getEntrantRaceByIds(newRacesList.stream().map(RaceRawData::getId).collect(Collectors.toList())).subscribe();
        return meetingDtoList;
    }

    public Flux<EntrantDto> getEntrantRaceByIds(List<String> raceIds) {
        return Flux.fromIterable(raceIds)
                .parallel() // create a parallel flux
                .runOn(Schedulers.parallel()) // specify which scheduler to use for the parallel execution
                .flatMap(this::getEntrantRaceById) // call the getRaceById method for each raceId
                .sequential(); // convert back to a sequential flux
    }

    public Flux<EntrantDto> getEntrantRaceById(String raceId) {
        try {
            LadBrokedItRaceDto raceDto = getLadBrokedItRaceDto(raceId);
            JsonObject results = raceDto.getResults();
            Map<String, Integer> positions = new HashMap<>();
            if(results !=null) {
                positions = results.keySet().stream().collect(Collectors.toMap(Function.identity(), key -> results.getAsJsonObject(key).get("position").getAsInt()));
            }
            else{
                positions.put("position",0);
            }
            HashMap<String, ArrayList<Float>> allEntrantPrices = raceDto.getPriceFluctuations();
            List<EntrantRawData> allEntrant = getListEntrant(raceDto, allEntrantPrices, raceId, positions);

            saveEntrant(allEntrant);
            return Flux.fromIterable(allEntrant)
                    .flatMap(r -> {
                        List<Float> entrantPrices = allEntrantPrices.get(r.getId());
                        EntrantDto entrantDto = EntrantMapper.toEntrantDto(r, entrantPrices);
                        return Mono.just(entrantDto);
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveMeeting(List<MeetingRawData> meetingRawData) {
        List<Meeting> newMeetings = meetingRawData.stream().map(MeetingMapper::toMeetingEntity).collect(Collectors.toList());
        Flux<Meeting> existedMeetings = meetingRepository
                .findAllByMeetingIdIn(newMeetings.stream().map(Meeting::getMeetingId).collect(Collectors.toList()));
        existedMeetings
                .collectList()
                .subscribe(existed ->
                        {
                            newMeetings.addAll(existed);
                            List<Meeting> meetingNeedUpdateOrInsert = newMeetings.stream().distinct().peek(m ->
                            {
                                if (m.getId() == null) {
                                    existed.stream()
                                            .filter(x -> x.getMeetingId().equals(m.getMeetingId()))
                                            .findFirst()
                                            .ifPresent(meeting -> m.setId(meeting.getId()));
                                }
                            }).filter(e -> !existed.contains(e)).collect(Collectors.toList());
                            log.info("Meeting need to be update or insert " + meetingNeedUpdateOrInsert.size());
                            meetingRepository.saveAll(meetingNeedUpdateOrInsert).subscribe();
                        }
                );
    }

    public void saveRace(List<RaceDto> raceDtoList) {
        List<Race> newRaces = raceDtoList.stream().map(MeetingMapper::toRaceEntity).collect(Collectors.toList());
        Flux<Race> existedRaces = raceRepository
                .findAllByRaceIdIn(newRaces.stream().map(Race::getRaceId).collect(Collectors.toList()));
        existedRaces
                .collectList()
                .subscribe(existed ->
                        {
                            newRaces.addAll(existed);
                            List<Race> raceNeedUpdateOrInsert = newRaces.stream().distinct().peek(e ->
                            {
                                if (e.getId() == null) {
                                    existed.stream()
                                            .filter(x -> x.getRaceId().equals(e.getRaceId()))
                                            .findFirst()
                                            .ifPresent(entrant -> e.setId(entrant.getId()));
                                }
                            }).filter(e -> !existed.contains(e)).collect(Collectors.toList());
                            log.info("Race need to be update is " + raceNeedUpdateOrInsert.size());
                            raceRepository.saveAll(raceNeedUpdateOrInsert).subscribe();
                        }
                );
    }

    public void saveEntrant(List<EntrantRawData> entrantRawData) {
        List<Entrant> newEntrants = entrantRawData.stream().map(MeetingMapper::toEntrantEntity).collect(Collectors.toList());
        Flux<Entrant> existedEntrant = entrantRepository
                .findAllByEntrantIdIn(entrantRawData.stream().map(EntrantRawData::getId).collect(Collectors.toList()));
        existedEntrant
                .collectList()
                .subscribe(existed ->
                        {
                            newEntrants.addAll(existed);
                            List<Entrant> entrantNeedUpdateOrInsert = newEntrants.stream().distinct().peek(e ->
                            {
                                if (e.getId() == null) {
                                    existed.stream()
                                            .filter(x -> x.getEntrantId().equals(e.getEntrantId()))
                                            .findFirst()
                                            .ifPresent(entrant -> e.setId(entrant.getId()));
                                }
                            }).filter(e -> !existed.contains(e)).collect(Collectors.toList());
                            log.info("Entrant need to be update is " + entrantNeedUpdateOrInsert.size());
                            entrantRepository.saveAll(entrantNeedUpdateOrInsert).subscribe();
                        }
                );
    }

    public List<EntrantRawData> getListEntrant(LadBrokedItRaceDto raceDto, HashMap<String, ArrayList<Float>> allEntrantPrices, String raceId, Map<String, Integer> positions) {
        List<EntrantRawData> allEntrant = raceDto.getEntrants().values().stream().filter(r -> r.getFormSummary() != null && r.getId() != null).map(r -> {
            List<Float> entrantPrices = allEntrantPrices == null ? new ArrayList<>() : allEntrantPrices.get(r.getId());
            Integer entrantPosition = positions.get(r.getId()) == null ? 0 : positions.get(r.getId());
            EntrantRawData entrantRawData = EntrantMapper.mapPrices(r, entrantPrices, entrantPosition);
            entrantRawData.setRaceId(raceId);
            return entrantRawData;
        }).collect(Collectors.toList());
        return allEntrant;
    }

    public LadBrokedItRaceDto getLadBrokedItRaceDto(String raceId) throws IOException {
        String url = AppConstant.LAD_BROKES_IT_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
        Response response = ApiUtils.get(url);
        JsonObject jsonObject = JsonParser.parseString(response.body().string()).getAsJsonObject();
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").create();
        LadBrokedItRaceDto raceDto = gson.fromJson(jsonObject.get("data"), LadBrokedItRaceDto.class);
        return raceDto;
    }
}