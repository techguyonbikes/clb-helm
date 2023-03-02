package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.service.repository.EntrantRepository;
import com.tvf.clb.service.repository.MeetingRepository;
import com.tvf.clb.service.repository.RaceRepository;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.model.EntrantRawData;
import com.tvf.clb.base.model.MeetingRawData;
import com.tvf.clb.base.model.RaceRawData;
import com.tvf.clb.base.model.VenueRawData;
import com.tvf.clb.base.utils.ApiUtils;
import com.tvf.clb.base.utils.AppConstant;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;


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
                //if null then throw error?
            } catch (IOException e) {
                //define exception
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
        for (MeetingRawData localMeeting : ausMeetings) {
            List<RaceRawData> localRace = ausRace.stream().filter(r -> localMeeting.getRaceIds().contains(r.getId()))
//                    .sorted(Comparator.comparing(Race::getNumber))
                    .collect(Collectors.toList());

            MeetingDto meetingDto = MeetingMapper.toMeetingDto(localMeeting, localRace);
            meetingDtoList.add(meetingDto);
        }
        saveMeeting(ausMeetings);
        saveRace(ausRace);
        return meetingDtoList;
    }

    public Flux<EntrantDto> getRaceById(String raceId) {
        String url = AppConstant.LAD_BROKES_IT_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
        try {
            Response response = ApiUtils.get(url);
            //todo check null
            JsonObject jsonObject = JsonParser.parseString(response.body().string()).getAsJsonObject();
            Gson gson = new Gson();
            LadBrokedItRaceDto raceDto = gson.fromJson(jsonObject.get("data"), LadBrokedItRaceDto.class);
            HashMap<String, ArrayList<Float>> allEntrantPrices = raceDto.getPriceFluctuations();
            List<EntrantRawData> allEntrant = raceDto.getEntrants().values().stream().filter(r -> r.getFormSummary() != null).map(r ->{
                List<Float> entrantPrices = allEntrantPrices.get(r.getId());
                EntrantRawData entrantRawData = EntrantMapper.mapPrices(r, entrantPrices);
                return entrantRawData;
            }).collect(Collectors.toList());

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

    //todo. Fix this,
    @Async()
    public void saveMeeting(List<MeetingRawData> meetingRawData) {
        List<Meeting> meetings = meetingRawData.stream().map(MeetingMapper::toMeetingEntity).collect(Collectors.toList());
        meetingRepository.saveAll(meetings).subscribe();
    }

    @Async()
    public void saveRace(List<RaceRawData> raceRawData) {
        List<Race> races = raceRawData.stream().map(MeetingMapper::toRaceEntity).collect(Collectors.toList());
        raceRepository.saveAll(races).subscribe();
    }

    public void saveEntrant(List<EntrantRawData> entrantRawData) {
        List<Entrant> entrants = entrantRawData.stream().map(MeetingMapper::toEntrantEntity).collect(Collectors.toList());
        entrantRepository.saveAll(entrants).subscribe();
    }



}
