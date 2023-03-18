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
import org.springframework.stereotype.Service;
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

@ClbService(componentType =  AppConstant.NED)
@Slf4j
public class NedsCrawlService implements ICrawlService{

    @Autowired
    private CrawUtils crawUtils;

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {
        return Mono.fromSupplier(() -> {
            LadBrokedItMeetingDto rawData = null;
            try {
                String url = AppConstant.NEDS_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
                Response response = ApiUtils.get(url);
                ResponseBody body = response.body();
                Gson gson = new GsonBuilder().setDateFormat(AppConstant.DATE_TIME_FORMAT_LONG).create();
                if (body != null) {
                    rawData = gson.fromJson(body.string(), LadBrokedItMeetingDto.class);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return getAllAusMeeting(rawData);
        }).flatMapMany(Flux::fromIterable);
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
        for (MeetingRawData localMeeting : ausMeetings) {
            List<RaceRawData> localRace = ausRace.stream().filter(r -> localMeeting.getRaceIds().contains(r.getId())).collect(Collectors.toList());
            MeetingDto meetingDto = MeetingMapper.toMeetingDto(localMeeting, localRace);
            meetingDtoList.add(meetingDto);
        }
        saveMeeting(ausMeetings);
        List<RaceDto> raceDtoList = meetingDtoList.stream().map(MeetingDto::getRaces).flatMap(List::stream).collect(Collectors.toList());
        saveRace(raceDtoList);
        getEntrantRaceByIds(ausRace.stream().map(RaceRawData::getId).collect(Collectors.toList())).subscribe();
        return meetingDtoList;
    }

    private Flux<EntrantDto> getEntrantRaceByIds(List<String> raceIds) {
        return Flux.fromIterable(raceIds)
                .parallel() // create a parallel flux
                .runOn(Schedulers.parallel()) // specify which scheduler to use for the parallel execution
                .flatMap(this::getEntrantByRaceId) // call the getRaceById method for each raceId
                .sequential(); // convert back to a sequential flux
    }

    public Flux<EntrantDto> getEntrantByRaceId(String raceId) {
        try {
            LadBrokedItRaceDto raceDto = getNedsRaceDto(raceId);
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

    public void saveMeeting(List<MeetingRawData> meetingRawData) {
        List<Meeting> newMeetings = meetingRawData.stream().map(MeetingMapper::toMeetingEntity).collect(Collectors.toList());
        crawUtils.saveMeetingSite(newMeetings, 2);
    }

    public void saveRace(List<RaceDto> raceDtoList) {
        List<Race> newRaces = raceDtoList.stream().map(MeetingMapper::toRaceEntity).collect(Collectors.toList());
        crawUtils.saveRaceSite(newRaces, 2);
    }

    public void saveEntrant(List<EntrantRawData> entrantRawData) {
        List<Entrant> newEntrants = entrantRawData.stream().map(MeetingMapper::toEntrantEntity).collect(Collectors.toList());

    }

    public List<EntrantRawData> getListEntrant(LadBrokedItRaceDto raceDto, Map<String, ArrayList<Float>> allEntrantPrices, String raceId, Map<String, Integer> positions) {
        return raceDto.getEntrants().values().stream().filter(r -> r.getFormSummary() != null && r.getId() != null).map(r -> {
            List<Float> entrantPrices = allEntrantPrices == null ? new ArrayList<>() : allEntrantPrices.get(r.getId());
            Integer entrantPosition = positions.get(r.getId()) == null ? 0 : positions.get(r.getId());
            EntrantRawData entrantRawData = EntrantMapper.mapPrices(r, entrantPrices, entrantPosition);
            entrantRawData.setRaceId(raceId);
            return entrantRawData;
        }).collect(Collectors.toList());
    }

    public LadBrokedItRaceDto getNedsRaceDto(String raceId) throws IOException {
        String url = AppConstant.NEDS_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
        Response response = ApiUtils.get(url);
        JsonObject jsonObject = JsonParser.parseString(response.body().string()).getAsJsonObject();
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(jsonObject.get("data"), LadBrokedItRaceDto.class);
    }
}
