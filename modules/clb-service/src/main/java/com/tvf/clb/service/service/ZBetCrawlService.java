package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.EntrantDto;
import com.tvf.clb.base.dto.EntrantMapper;
import com.tvf.clb.base.dto.MeetingDto;
import com.tvf.clb.base.dto.MeetingMapper;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.model.CrawlEntrantData;
import com.tvf.clb.base.model.EntrantRawData;
import com.tvf.clb.base.model.zbet.*;
import com.tvf.clb.base.utils.ApiUtils;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.ConvertBase;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@ClbService(componentType = AppConstant.ZBET)
@Slf4j
public class ZBetCrawlService implements ICrawlService {

    @Autowired
    private CrawUtils crawUtils;

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {
        return Mono.fromSupplier(() -> {
            List<ZBetMeetingRawData> rawData = null;
            try {
                Thread.sleep(20000);
                String url = AppConstant.ZBET_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
                Response response = ApiUtils.get(url);
                ResponseBody body = response.body();
                JsonObject jsonObject = JsonParser.parseString(response.body().string()).getAsJsonObject();
                Gson gson = new GsonBuilder().create();
                if (body != null)
                    rawData = gson.fromJson(jsonObject.get("data"), new TypeToken<List<ZBetMeetingRawData>>() {
                    }.getType());
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log.info("Start getting the API from ZBet.");
            return getAllAusMeeting(rawData, date);
        }).flatMapMany(Flux::fromIterable);
    }

    @Override
    public Map<Integer, CrawlEntrantData> getEntrantByRaceUUID(String raceId) {
        ZBetRaceRawData raceDto = getZBetRaceData(raceId);
        if (raceDto != null) {
            List<EntrantRawData> allEntrant = getListEntrant(raceId, raceDto);
            Map<Integer, CrawlEntrantData> result = new HashMap<>();
            allEntrant.forEach(x -> {
                Map<Integer, List<Float>> priceFluctuations = new HashMap<>();
                priceFluctuations.put(AppConstant.ZBET_SITE_ID, x.getPriceFluctuations());
                result.put(x.getNumber(), new CrawlEntrantData(x.getPosition(), null, AppConstant.ZBET_SITE_ID, priceFluctuations));
            });
            return result;
        } else {
            log.error("Can not found ZBet race by RaceId " + raceId);
        }
        return null;
    }


    public List<EntrantRawData> getListEntrant(String raceId, ZBetRaceRawData raceDto) {
        Map<Long, Integer> position = raceDto.getDisplayedResults().stream()
                .collect(Collectors.toMap(ZBetResultsRawData::getSelection_id, ZBetResultsRawData::getPosition));

        return raceDto.getSelections().stream().filter(f -> f.getName() != null && f.getNumber() != null)
                .map(m -> EntrantMapper.mapCrawlEntrant(raceId, m, buildPriceFluctuations(m), position)).collect(Collectors.toList());
    }

    private List<MeetingDto> getAllAusMeeting(List<ZBetMeetingRawData> zBetMeeting, LocalDate date) {
        List<ZBetMeetingRawData> ausZBetMeeting = zBetMeeting.stream().filter(zBetMeetingRaw ->
                AppConstant.VALID_COUNTRY_CODE.contains(zBetMeetingRaw.getCountry())).collect(Collectors.toList());
        saveMeetingSite(ausZBetMeeting);

        List<ZBetRacesData> racesData = new ArrayList<>();
        ausZBetMeeting.forEach(meeting -> {

            List<ZBetRacesData> meetingRaces = meeting.getRaces();
            meetingRaces.forEach(race -> {
                race.setMeetingName(meeting.getName());
                race.setType(ConvertBase.convertRaceTypeOfTab(race.getType()));
            });
            racesData.addAll(meetingRaces);

            saveRaceSite(racesData, meeting);
        });

        crawlAndSaveEntrants(racesData, date).subscribe();
        return Collections.emptyList();
    }

    private Flux<EntrantDto> crawlAndSaveEntrants(List<ZBetRacesData> raceDtoList, LocalDate date) {
        return Flux.fromIterable(raceDtoList)
                .parallel() // create a parallel flux
                .runOn(Schedulers.parallel()) // specify which scheduler to use for the parallel execution
                .flatMap(race -> crawlAndSaveEntrantsInRace(race, date)) // call the getRaceById method for each raceId
                .sequential(); // convert back to a sequential flux
    }

    public Flux<EntrantDto> crawlAndSaveEntrantsInRace(ZBetRacesData race, LocalDate date) {
        String raceUUID = race.getId().toString();

        ZBetRaceRawData raceDto = getZBetRaceData(raceUUID);
        if (raceDto != null) {
            List<ZBetEntrantData> allEntrant = raceDto.getSelections();

            saveEntrant(allEntrant, race, date);
        } else {
            log.error("Can not found ZBet race by RaceUUID " + raceUUID);
        }

        return Flux.empty();
    }

    public void saveMeetingSite(List<ZBetMeetingRawData> meetingRawData) {
        List<Meeting> newMeetings = meetingRawData.stream().map(MeetingMapper::toMeetingEntity).collect(Collectors.toList());
        crawUtils.saveMeetingSite(newMeetings, AppConstant.ZBET_SITE_ID);
    }

    public void saveRaceSite(List<ZBetRacesData> raceDtoList, ZBetMeetingRawData meeting) {
        List<Race> newRaces = raceDtoList.stream().map(MeetingMapper::toRaceEntity).collect(Collectors.toList());
        crawUtils.saveRaceSite(newRaces, AppConstant.ZBET_SITE_ID, MeetingMapper.toMeetingEntity(meeting));
    }

    public void saveEntrant(List<ZBetEntrantData> entrantRawData, ZBetRacesData race, LocalDate date) {

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(AppConstant.DATE_TIME_PATTERN);

        Instant startTime = LocalDateTime.parse(race.getStartDate(), dtf).atZone(AppConstant.AU_ZONE_ID).toInstant();

        List<Entrant> newEntrants = entrantRawData.stream().distinct()
                .map(meeting -> MeetingMapper.toEntrantEntity(meeting, buildPriceFluctuations(meeting))).collect(Collectors.toList());

        String raceIdIdentifierInRedis = String.format("%s - %s - %s - %s", race.getMeetingName(), race.getNumber(), race.getType(), date);

        crawUtils.saveEntrantIntoRedis(newEntrants, AppConstant.ZBET_SITE_ID, raceIdIdentifierInRedis, race.getId().toString(),
                null, startTime, race.getNumber(), race.getType());

        crawUtils.saveEntrantsPriceIntoDB(newEntrants, MeetingMapper.toRaceDto(race) ,AppConstant.ZBET_SITE_ID);
    }

    private ZBetRaceRawData getZBetRaceData(String raceId) {
        try {
            String url = AppConstant.ZBET_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
            Response response = ApiUtils.get(url);
            JsonObject jsonObject = JsonParser.parseString(response.body().string()).getAsJsonObject();
            Gson gson = new GsonBuilder().create();
            return gson.fromJson(jsonObject.get("data"), ZBetRaceRawData.class);
        } catch (IOException e) {
            log.error("Got error while get ZBet Race Data raceId: " + raceId);
            log.error(e.getMessage());
        }
        return new ZBetRaceRawData();
    }

    private List<Float> buildPriceFluctuations(ZBetEntrantData entrantData) {
        if (entrantData.getPrices() instanceof JsonObject) {
            Map<Integer, ZBetPrices> pricesMap = new Gson().fromJson(entrantData.getPrices(), new TypeToken<Map<Integer, ZBetPrices>>() {
            }.getType());
            if (!pricesMap.isEmpty()) {
                List<ZBetPrices> listZBF = pricesMap.values().stream().filter(zBetPrices -> zBetPrices.getProductCode().equals("ZBF"))
                        .sorted(Comparator.comparing(ZBetPrices::getRequestedAt)).collect(Collectors.toList());
                List<String> lastFluctuations = Arrays.stream(listZBF.get(listZBF.size() - 1).getFluctuations().split(",")).collect(Collectors.toList());
                return lastFluctuations.stream().map(Float::parseFloat).filter(x -> x != 0).collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}
