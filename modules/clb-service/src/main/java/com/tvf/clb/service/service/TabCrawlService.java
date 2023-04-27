package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.dto.tab.TabBetMeetingDto;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.exception.ApiRequestFailedException;
import com.tvf.clb.base.model.CrawlEntrantData;
import com.tvf.clb.base.model.CrawlRaceData;
import com.tvf.clb.base.model.EntrantRawData;
import com.tvf.clb.base.model.tab.TabMeetingRawData;
import com.tvf.clb.base.model.tab.TabRacesData;
import com.tvf.clb.base.model.tab.TabRunnerRawData;
import com.tvf.clb.base.utils.ApiUtils;
import com.tvf.clb.base.utils.AppConstant;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@ClbService(componentType = AppConstant.TAB)
@Slf4j
public class TabCrawlService implements ICrawlService{

    @Autowired
    private CrawUtils crawUtils;

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {
        log.info("Start getting the API from TAB.");

        CrawlMeetingFunction crawlFunction = crawlDate -> {
            String url = AppConstant.TAB_BET_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
            Response response = ApiUtils.get(url);
            ResponseBody body = response.body();
            Gson gson = new GsonBuilder().setDateFormat(AppConstant.DATE_TIME_FORMAT_LONG).create();
            if (body != null) {
                TabBetMeetingDto rawData = gson.fromJson(body.string(), TabBetMeetingDto.class);
                return getAllAusMeeting(rawData,date);
            }

            return null;
        };

        return crawUtils.crawlMeeting(crawlFunction, date, 20000L, this.getClass().getName());
    }

    private List<MeetingDto> getAllAusMeeting(TabBetMeetingDto meetingRawData,LocalDate date) {
        List<TabMeetingRawData> newTabMeetingRawData = meetingRawData.getMeetings().stream().filter(m -> AppConstant.VALID_LOCATION_CODE.contains(m.getLocation())).collect(Collectors.toList());
        List<MeetingDto> meetingDtoList = new ArrayList<>();
        for (TabMeetingRawData localMeeting : newTabMeetingRawData) {
            List<TabRacesData> localRace =localMeeting.getRaces();
            MeetingDto meetingDto =MeetingMapper.toMeetingTABDto(localMeeting,localRace);
            meetingDtoList.add(meetingDto);
        }
        List<RaceDto> raceDtoList = meetingDtoList.stream().map(MeetingDto::getRaces).flatMap(List::stream).collect(Collectors.toList());
        saveMeeting(newTabMeetingRawData);
        saveRace(raceDtoList);
        crawlAndSaveEntrants(raceDtoList, date).subscribe();
        return meetingDtoList;
    }
    public void saveMeeting(List<TabMeetingRawData> meetingRawData) {
        List<Meeting> newMeetings = meetingRawData.stream().map(MeetingMapper::toMeetingEntityFromTab).collect(Collectors.toList());
        crawUtils.saveMeetingSite(newMeetings, AppConstant.TAB_SITE_ID);
    }

    public void saveRace(List<RaceDto> raceDtoList) {
        List<Race> newRaces = raceDtoList.stream().map(MeetingMapper::toRaceEntity).collect(Collectors.toList());
        crawUtils.saveRaceSite(newRaces, AppConstant.TAB_SITE_ID);
    }

    @Override
    public CrawlRaceData getEntrantByRaceUUID(String raceId) {
        TabRunnerRawData runnerRawData = crawlRunnerDataTAB(raceId);

        if (runnerRawData == null) {
            return new CrawlRaceData();
        }

        // TODO fix this bug, sometime api return null because of wrong race UUID
        if(runnerRawData.getRunners() == null && runnerRawData.getResults() == null) {
            log.debug("Null runner data at site Tab, raceUUID = {}: ", raceId);
            return new CrawlRaceData();
        }

        List<EntrantRawData> allEntrant = getListEntrant(raceId, runnerRawData);

        Map<Integer, CrawlEntrantData> mapEtrants = new HashMap<>();
        allEntrant.forEach(x -> {
            Map<Integer, List<Float>> priceFluctuations = new HashMap<>();
            priceFluctuations.put(AppConstant.TAB_SITE_ID, x.getPriceFluctuations());
            mapEtrants.put(x.getNumber(), new CrawlEntrantData(x.getPosition(), priceFluctuations));
        });

        CrawlRaceData result = new CrawlRaceData();
        result.setSiteId(SiteEnum.TAB.getId());
        result.setMapEntrants(mapEtrants);

        if (isRaceStatusFinal(runnerRawData)) {
            String finalResult = runnerRawData.getResults().stream().map(Object::toString).collect(Collectors.joining(","));
            result.setFinalResult(Collections.singletonMap(AppConstant.TAB_SITE_ID, finalResult));
        }

        return result;
    }

    @Override
    public Flux<EntrantDto> crawlAndSaveEntrantsInRace(RaceDto raceDto, LocalDate date) {

        String raceUUID = raceDto.getId();
        TabRunnerRawData runnerRawData = crawlRunnerDataTAB(raceUUID);
        // TODO fix this bug, sometime api return null because of wrong race UUID

        if (runnerRawData != null) {
            if (runnerRawData.getResults() == null && runnerRawData.getRunners() == null) {
                log.debug("Site tab get Entrant by raceUUID not found: "+raceUUID);
                return Flux.empty();
            }
            List<EntrantRawData> allEntrant = getListEntrant(raceUUID, runnerRawData);

            if (isRaceStatusFinal(runnerRawData)) {
                String finalResult = runnerRawData.getResults().stream().map(Object::toString).collect(Collectors.joining(","));
                raceDto.setDistance(runnerRawData.getRaceDistance());
                crawUtils.updateRaceFinalResultIntoDB(raceDto, AppConstant.TAB_SITE_ID, finalResult);
            }
            raceDto.setDistance(runnerRawData.getRaceDistance());
            saveEntrant(allEntrant, String.format("%s - %s - %s - %s", raceDto.getMeetingName(), raceDto.getNumber(),
                    raceDto.getRaceType(), date), raceDto);
            return Flux.fromIterable(allEntrant)
                    .flatMap(r -> Mono.just(EntrantMapper.toEntrantDto(r)));

        } else {
            crawUtils.saveFailedCrawlRace(this.getClass().getName(), raceDto, date);
            throw new ApiRequestFailedException();
        }
    }

    private boolean isRaceStatusFinal(TabRunnerRawData runnerRawData) {
        return runnerRawData.getRaceStatus() != null
                && AppConstant.TAB_RACE_STATUS_FINAL.equalsIgnoreCase(runnerRawData.getRaceStatus());
    }

    public void saveEntrant(List<EntrantRawData> entrantRawData, String raceName, RaceDto raceDto) {
        List<Entrant> newEntrants = entrantRawData.stream().distinct().map(MeetingMapper::toEntrantEntity).collect(Collectors.toList());
        crawUtils.saveEntrantCrawlDataToRedis(newEntrants, AppConstant.TAB_SITE_ID, raceName, raceDto);
    }

    private List<EntrantRawData> getListEntrant(String raceId, TabRunnerRawData runnerRawData) {
        return runnerRawData.getRunners().stream().filter(f -> f.getFixedOdds() != null)
                .map(x -> {
                    List<Float> listPrice = x.getFixedOdds().getFlucs() == null ? new ArrayList<>() :
                            x.getFixedOdds().getFlucs().stream().map(f -> f.getReturnWin() == null ? 0f : f.getReturnWin())
                            .collect(Collectors.toList());
                    return EntrantMapper.toEntrantRawData(x, runnerRawData.getResults(), listPrice, raceId);
                }).collect(Collectors.toList());
    }

    public TabRunnerRawData crawlRunnerDataTAB(String raceId) {

        CrawlRaceFunction crawlFunction = raceUUID -> {
            String url = AppConstant.TAB_BET_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
            Response response = ApiUtils.get(url);
            JsonObject jsonObject = JsonParser.parseString(response.body().string()).getAsJsonObject();
            Gson gson = new GsonBuilder().create();
            return gson.fromJson(jsonObject, TabRunnerRawData.class);
        };

        return (TabRunnerRawData) crawUtils.crawlRace(crawlFunction, raceId, this.getClass().getName());
    }

}
