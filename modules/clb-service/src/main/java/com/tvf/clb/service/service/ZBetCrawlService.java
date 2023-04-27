package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.exception.ApiRequestFailedException;
import com.tvf.clb.base.model.CrawlEntrantData;
import com.tvf.clb.base.model.CrawlRaceData;
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

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@ClbService(componentType = AppConstant.ZBET)
@Slf4j
public class ZBetCrawlService implements ICrawlService {

    @Autowired
    private CrawUtils crawUtils;

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {
        log.info("Start getting the API from ZBet.");

        CrawlMeetingFunction crawlFunction = crawlDate -> {
            String url = AppConstant.ZBET_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
            Response response = ApiUtils.get(url);
            ResponseBody body = response.body();
            JsonObject jsonObject = JsonParser.parseString(response.body().string()).getAsJsonObject();
            Gson gson = new GsonBuilder().create();
            if (body != null) {
                List<ZBetMeetingRawData> rawData = gson.fromJson(jsonObject.get("data"), new TypeToken<List<ZBetMeetingRawData>>() {}.getType());
                return getAllAusMeeting(rawData, date);
            }

            return null;
        };

        return crawUtils.crawlMeeting(crawlFunction, date, 20000L, this.getClass().getName());
    }

    @Override
    public CrawlRaceData getEntrantByRaceUUID(String raceId) {
        ZBetRaceRawData raceDto = getZBetRaceData(raceId);
        if (raceDto != null) {
            List<EntrantRawData> allEntrant = getListEntrant(raceId, raceDto);

            Map<Integer, CrawlEntrantData> mapEntrants = new HashMap<>();
            allEntrant.forEach(x -> {
                Map<Integer, List<Float>> priceFluctuations = new HashMap<>();
                priceFluctuations.put(AppConstant.ZBET_SITE_ID, x.getPriceFluctuations());
                mapEntrants.put(x.getNumber(), new CrawlEntrantData(x.getPosition(), priceFluctuations));
            });

            CrawlRaceData result = new CrawlRaceData();
            result.setSiteId(SiteEnum.ZBET.getId());
            result.setStatus(ConvertBase.getZBetRaceStatus(raceDto.getStatus()));
            result.setMapEntrants(mapEntrants);

            if (AppConstant.STATUS_FINAL.equals(result.getStatus()) && raceDto.getFinalResult() != null) {
                String raceFinalResult = raceDto.getFinalResult().replace('/', ',');
                result.setFinalResult(Collections.singletonMap(AppConstant.ZBET_SITE_ID, raceFinalResult));
            }

            return result;

        } else {
            return new CrawlRaceData();
        }
    }


    public List<EntrantRawData> getListEntrant(String raceId, ZBetRaceRawData raceDto) {

        Map<Integer, Integer> positionResult = crawUtils.getPositionInResult(raceDto.getFinalResult());

        return raceDto.getSelections().stream().filter(f -> f.getName() != null && f.getNumber() != null)
                .map(m -> EntrantMapper.mapCrawlEntrant(raceId, m, buildPriceFluctuations(m), positionResult)).collect(Collectors.toList());
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
                race.setStatus(ConvertBase.getZBetRaceStatus(race.getStatus()));
            });
            racesData.addAll(meetingRaces);
        });

        List<RaceDto> raceDtoList = racesData.stream().map(MeetingMapper::toRaceDto).collect(Collectors.toList());

        saveRaceSite(raceDtoList);

        crawlAndSaveEntrants(raceDtoList, date).subscribe();
        return Collections.emptyList();
    }

    @Override
    public Flux<EntrantDto> crawlAndSaveEntrantsInRace(RaceDto raceDto, LocalDate date) {
        String raceUUID = raceDto.getId();

        ZBetRaceRawData raceRawData = getZBetRaceData(raceUUID);
        if (raceRawData != null) {
            List<ZBetEntrantData> allEntrant = raceRawData.getSelections();
            raceDto.setDistance(raceRawData.getDistance());
            if (AppConstant.STATUS_FINAL.equals(raceDto.getStatus()) && raceRawData.getFinalResult() != null) {
                crawUtils.updateRaceFinalResultIntoDB(raceDto, AppConstant.ZBET_SITE_ID, raceRawData.getFinalResult().replace('/', ','));
            }

            saveEntrant(allEntrant, raceDto, date);

        } else {
            crawUtils.saveFailedCrawlRace(this.getClass().getName(), raceDto, date);
            throw new ApiRequestFailedException();
        }

        return Flux.empty();
    }

    public void saveMeetingSite(List<ZBetMeetingRawData> meetingRawData) {
        List<Meeting> newMeetings = meetingRawData.stream().map(MeetingMapper::toMeetingEntity).collect(Collectors.toList());
        crawUtils.saveMeetingSite(newMeetings, AppConstant.ZBET_SITE_ID);
    }

    public void saveRaceSite(List<RaceDto> raceDtoList) {
        crawUtils.saveRaceSiteAndUpdateStatus(raceDtoList, AppConstant.ZBET_SITE_ID);
    }

    public void saveEntrant(List<ZBetEntrantData> entrantRawData, RaceDto raceDto, LocalDate date) {

        List<Entrant> newEntrants = entrantRawData.stream().distinct()
                .map(meeting -> MeetingMapper.toEntrantEntity(meeting, buildPriceFluctuations(meeting))).collect(Collectors.toList());

        String raceIdIdentifierInRedis = String.format("%s - %s - %s - %s", raceDto.getMeetingName(), raceDto.getNumber(), raceDto.getRaceType(), date);

        crawUtils.saveEntrantCrawlDataToRedis(newEntrants, AppConstant.ZBET_SITE_ID, raceIdIdentifierInRedis, raceDto);

        crawUtils.saveEntrantsPriceIntoDB(newEntrants, raceDto, AppConstant.ZBET_SITE_ID);
    }

    private ZBetRaceRawData getZBetRaceData(String raceId) {

        CrawlRaceFunction crawlFunction = raceUUID -> {
            String url = AppConstant.ZBET_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
            Response response = ApiUtils.get(url);
            JsonObject jsonObject = JsonParser.parseString(response.body().string()).getAsJsonObject();
            Gson gson = new GsonBuilder().create();
            return gson.fromJson(jsonObject.get("data"), ZBetRaceRawData.class);
        };

        return (ZBetRaceRawData) crawUtils.crawlRace(crawlFunction, raceId, this.getClass().getName());
    }

    private List<Float> buildPriceFluctuations(ZBetEntrantData entrantData) {
        if (entrantData.getPrices() instanceof JsonObject) {
            Map<Integer, ZBetPrices> pricesMap = new Gson().fromJson(entrantData.getPrices(), new TypeToken<Map<Integer, ZBetPrices>>() {
            }.getType());
            if (!pricesMap.isEmpty()) {
                List<ZBetPrices> listZBF = pricesMap.values().stream().filter(zBetPrices -> AppConstant.VALID_CHECK_PRODUCT_CODE.equals(zBetPrices.getProductCode()))
                        .sorted(Comparator.comparing(ZBetPrices::getRequestedAt)).collect(Collectors.toList());
                List<String> lastFluctuations = Arrays.stream(listZBF.get(listZBF.size() - 1).getFluctuations().split(",")).collect(Collectors.toList());
                return lastFluctuations.stream().map(Float::parseFloat).filter(x -> x != 0).collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}
