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
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
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
                Thread.sleep(20000);
                String url = AppConstant.NEDS_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
                Response response = ApiUtils.get(url);
                ResponseBody body = response.body();
                Gson gson = new GsonBuilder().setDateFormat(AppConstant.DATE_TIME_FORMAT_LONG).create();
                if (body != null) {
                    rawData = gson.fromJson(body.string(), LadBrokedItMeetingDto.class);
                }
            } catch (IOException e) {
                throw new ApiRequestFailedException("API request failed: " + e.getMessage(), e);
            } catch (InterruptedException e) {
                log.warn("Interrupted for 20 seconds!");
                Thread.currentThread().interrupt();
            }
            log.info("Start getting the API from Ned.");
            return getAllAusMeeting(rawData, date);
        }).flatMapMany(Flux::fromIterable);
    }

    @Override
    public CrawlRaceData getEntrantByRaceUUID(String raceId) {
        try {
            LadBrokedItRaceDto raceDto = getNedsRaceDto(raceId);
            JsonObject results = raceDto.getResults();
            Map<String, Integer> positions = new HashMap<>();
            if (results != null) {
                positions = results.keySet().stream().collect(Collectors.toMap(Function.identity(), key -> results.getAsJsonObject(key).get(AppConstant.POSITION).getAsInt()));
            } else {
                positions.put(AppConstant.POSITION, 0);
            }
            HashMap<String, ArrayList<Float>> allEntrantPrices = raceDto.getPriceFluctuations();
            List<EntrantRawData> allEntrant = crawUtils.getListEntrant(raceDto, allEntrantPrices, raceId, positions);

            Map<Integer, CrawlEntrantData> mapEntrants = new HashMap<>();
            allEntrant.forEach(x -> {
                List<Float> entrantPrice = allEntrantPrices.get(x.getId()) == null ? new ArrayList<>()
                        : new ArrayList<>(allEntrantPrices.get(x.getId()));
                Map<Integer, List<Float>> priceFluctuations = new HashMap<>();
                priceFluctuations.put(AppConstant.NED_SITE_ID, entrantPrice);
                mapEntrants.put(x.getNumber(), new CrawlEntrantData(x.getPosition(), priceFluctuations));
            });

            CrawlRaceData result = new CrawlRaceData();
            result.setSiteId(SiteEnum.NED.getId());
            result.setMapEntrants(mapEntrants);

            return result;
        } catch (IOException e) {
            throw new ApiRequestFailedException("API request failed: " + e.getMessage(), e);
        }
    }

    private List<MeetingDto> getAllAusMeeting(LadBrokedItMeetingDto ladBrokedItMeetingDto, LocalDate date) {
        List<VenueRawData> ausVenues = ladBrokedItMeetingDto.getVenues().values().stream().filter(v -> AppConstant.VALID_COUNTRY_CODE.contains(v.getCountry())).collect(Collectors.toList());
        List<String> venuesId = ausVenues.stream().map(VenueRawData::getId).collect(Collectors.toList());

        Map<String, String> meetingState = ausVenues.stream().collect(Collectors.toMap(VenueRawData::getId, VenueRawData::getState));

        List<MeetingRawData> meetings = new ArrayList<>(ladBrokedItMeetingDto.getMeetings().values());
        List<MeetingRawData> ausMeetings = meetings.stream().filter(m -> venuesId.contains(m.getVenueId()) && m.getTrackCondition() != null)
                .peek(x -> x.setState(meetingState.get(x.getVenueId()))).collect(Collectors.toList());
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
        List<RaceDto> raceDtoList = meetingDtoList.stream().map(MeetingDto::getRaces).flatMap(List::stream).filter(x -> x.getNumber() != null).collect(Collectors.toList());
        saveRace(raceDtoList);
        crawlAndSaveEntrants(raceDtoList,  date).subscribe();
        return meetingDtoList;
    }

    private Flux<EntrantDto> crawlAndSaveEntrants(List<RaceDto> raceDtoList, LocalDate date) {
        return Flux.fromIterable(raceDtoList)
                .parallel() // create a parallel flux
                .runOn(Schedulers.parallel()) // specify which scheduler to use for the parallel execution
                .flatMap(raceDto -> crawlAndSaveEntrantsInRace(raceDto, date)) // call the getRaceById method for each raceId
                .sequential(); // convert back to a sequential flux
    }

    public Flux<EntrantDto> crawlAndSaveEntrantsInRace(RaceDto raceDto, LocalDate date) {
        try {
            String raceUUID = raceDto.getId();
            LadBrokedItRaceDto raceRawData = getNedsRaceDto(raceUUID);
            JsonObject results = raceRawData.getResults();
            Map<String, Integer> positions = new HashMap<>();
            if (results != null) {
                positions = results.keySet().stream().collect(Collectors.toMap(Function.identity(), key -> results.getAsJsonObject(key).get(AppConstant.POSITION).getAsInt()));
            } else {
                positions.put(AppConstant.POSITION, 0);
            }
            HashMap<String, ArrayList<Float>> allEntrantPrices = raceRawData.getPriceFluctuations();
            List<EntrantRawData> allEntrant = crawUtils.getListEntrant(raceRawData, allEntrantPrices, raceUUID, positions);
            saveEntrant(allEntrant, raceDto, date);
            return Flux.fromIterable(allEntrant)
                    .flatMap(r -> {
                        List<Float> entrantPrices = CollectionUtils.isEmpty(allEntrantPrices) ? new ArrayList<>() : allEntrantPrices.get(r.getId());
                        EntrantDto entrantDto = EntrantMapper.toEntrantDto(r, entrantPrices);
                        return Mono.just(entrantDto);
                    });
        } catch (IOException e) {
            throw new ApiRequestFailedException("API request failed: " + e.getMessage(), e);
        }
    }

    public void saveMeeting(List<MeetingRawData> meetingRawData) {
        List<Meeting> newMeetings = meetingRawData.stream().map(MeetingMapper::toMeetingEntity).collect(Collectors.toList());
        crawUtils.saveMeetingSite(newMeetings, AppConstant.NED_SITE_ID);
    }

    public void saveRace(List<RaceDto> raceDtoList) {
        List<Race> newRaces = raceDtoList.stream().map(MeetingMapper::toRaceEntity).collect(Collectors.toList());
        crawUtils.saveRaceSite(newRaces, AppConstant.NED_SITE_ID);
    }

    public void saveEntrant(List<EntrantRawData> entrantRawData, RaceDto raceDto, LocalDate date) {
        List<Entrant> newEntrants = entrantRawData.stream().distinct().map(MeetingMapper::toEntrantEntity).collect(Collectors.toList());

        String raceIdIdentifierInRedis = String.format("%s - %s - %s - %s", raceDto.getMeetingName(), raceDto.getNumber(), raceDto.getRaceType(), date);
        crawUtils.saveEntrantIntoRedis(newEntrants, AppConstant.NED_SITE_ID, raceIdIdentifierInRedis, raceDto.getId(),
                null, raceDto.getAdvertisedStart(), raceDto.getNumber(), raceDto.getRaceType());

        crawUtils.saveEntrantsPriceIntoDB(newEntrants, raceDto, AppConstant.NED_SITE_ID);
    }


    public LadBrokedItRaceDto getNedsRaceDto(String raceId) throws IOException {
        String url = AppConstant.NEDS_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
        Response response = ApiUtils.get(url);
        JsonObject jsonObject = JsonParser.parseString(response.body().string()).getAsJsonObject();
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(jsonObject.get("data"), LadBrokedItRaceDto.class);
    }
}
