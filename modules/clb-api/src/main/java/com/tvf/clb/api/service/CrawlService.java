package com.tvf.clb.api.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tvf.clb.api.repository.MeetingRepository;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.model.Entrant;
import com.tvf.clb.base.model.Meeting;
import com.tvf.clb.base.model.Race;
import com.tvf.clb.base.model.Venue;
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
import java.util.stream.Collectors;


@Service
@Slf4j
public class CrawlService {

    @Autowired
    private MeetingRepository meetingRepository;

    public Mono<List<MeetingDto>> getTodayMeetings(LocalDate date) {
        return Mono.fromSupplier(() -> {
            LadBrokedItMeetingDto rawData;
            try {
                String url = AppConstant.LAD_BROKES_IT_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
                Response response = ApiUtils.get(url);
                ResponseBody body = response.body();
                Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").create();
                rawData = gson.fromJson(body.string(), LadBrokedItMeetingDto.class);
                log.info("Test");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return getAllAusMeeting(rawData);
        });
    }

    private List<MeetingDto> getAllAusMeeting(LadBrokedItMeetingDto ladBrokedItMeetingDto) {
        List<Venue> ausVenues = ladBrokedItMeetingDto.getVenues().values().stream().filter(v -> v.getCountry().equals(AppConstant.AUS)).collect(Collectors.toList());
        List<String> venuesId = ausVenues.stream().map(Venue::getId).collect(Collectors.toList());
        List<Meeting> meetings = new ArrayList<>(ladBrokedItMeetingDto.getMeetings().values());
        List<Meeting> ausMeetings = meetings.stream().filter(m -> StringUtils.hasText(m.getCountry()) && venuesId.contains(m.getVenueId())).collect(Collectors.toList());
        List<String> raceIds = ausMeetings.stream().map(Meeting::getRaceIds).flatMap(List::stream)
                .collect(Collectors.toList());
        List<Race> ausRace = ladBrokedItMeetingDto.getRaces()
                .values().stream().filter(r -> raceIds.contains(r.getId())).collect(Collectors.toList());
        List<MeetingDto> meetingDtoList = new ArrayList<>();
        for (Meeting localMeeting : ausMeetings) {
            List<Race> localRace = ausRace.stream().filter(r -> localMeeting.getRaceIds().contains(r.getId()))
//                    .sorted(Comparator.comparing(Race::getNumber))
                    .collect(Collectors.toList());

            MeetingDto meetingDto = MeetingDtoMapper.toMeetingDto(localMeeting, localRace);
            meetingDtoList.add(meetingDto);
        }
        saveMeeting(ausMeetings);
        return meetingDtoList;
    }

    public Flux<EntrantDto> getRaceById(String raceId) {
        String url = AppConstant.LAD_BROKES_IT_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
        try {
            Response response = ApiUtils.get(url);
            JsonObject jsonObject = JsonParser.parseString(response.body().string()).getAsJsonObject();
            Gson gson = new Gson();
            LadBrokedItRaceDto raceDto = gson.fromJson(jsonObject.get("data"), LadBrokedItRaceDto.class);
            HashMap<String, ArrayList<Float>> allEntrantPrices = raceDto.getPriceFluctuations();
            List<Entrant> allEntrant = raceDto.getEntrants().values().stream().filter(r -> r.getFormSummary() != null).collect(Collectors.toList());
            return Flux.fromIterable(allEntrant)
                    .flatMap(r -> {
                        List<Float> entrantPrices = allEntrantPrices.get(r.getId());
                        EntrantDto entrantDto = EntrantDtoMapper.toEntrantDto(r, entrantPrices);
                        return Mono.just(entrantDto);
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //todo. Fix this,
    @Async()
    public void saveMeeting(List<Meeting> meetings) {
        meetingRepository.saveAll(meetings).subscribe();
    }
}
