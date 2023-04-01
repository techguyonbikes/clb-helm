package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.exception.ApiRequestFailedException;
import com.tvf.clb.base.model.CrawlEntrantData;
import com.tvf.clb.base.model.EntrantRawData;
import com.tvf.clb.base.model.tab.TabRunnerRawData;
import com.tvf.clb.base.utils.ApiUtils;
import com.tvf.clb.base.utils.AppConstant;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ClbService(componentType = AppConstant.TAB)
@Slf4j
public class TabCrawlService implements ICrawlService {

    @Autowired
    private CrawUtils crawUtils;

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {
        return null;
    }

    @Override
    public Map<Long, CrawlEntrantData> getEntrantByRaceUUID(String raceId, Map<String, Long> entrantIdMapName) {
        try {
            TabRunnerRawData runnerRawData = crawlRunnerDataTAB(raceId);
            // TODO fix this bug, sometime api return null because of wrong race UUID
            if (runnerRawData.getRunners() == null && runnerRawData.getResults() == null) {
                return new HashMap<>();
            }
            List<EntrantRawData> allEntrant = getListEntrant(raceId, runnerRawData);
            Map<Long, CrawlEntrantData> result = new HashMap<>();
            allEntrant.forEach(x -> {
                Map<Integer, List<Float>> priceFluctuations = new HashMap<>();
                priceFluctuations.put(AppConstant.TAB_SITE_ID, x.getPriceFluctuations());
                result.put(entrantIdMapName.get(x.getName() + " - " + x.getNumber()), new CrawlEntrantData(x.getPosition(), priceFluctuations));
            });
            return result;
        } catch (IOException e) {
            throw new ApiRequestFailedException("API request failed: " + e.getMessage(), e);
        }
    }

    public Flux<EntrantDto> crawlAndSaveAllEntrants(List<RaceDto> raceDtoList, LocalDate date) {
        return Flux.fromIterable(raceDtoList)
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(raceDto -> crawlAndSaveEntrantsInRace(raceDto, date))
                .sequential();
    }

    public Flux<EntrantDto> crawlAndSaveEntrantsInRace(RaceDto raceDto, LocalDate date) {
        try {
            String raceUUID = raceDto.getId();
            TabRunnerRawData runnerRawData = crawlRunnerDataTAB(raceUUID);
            // TODO fix this bug, sometime api return null because of wrong race UUID
            if (runnerRawData.getResults() == null && runnerRawData.getRunners() == null) {
                return Flux.empty();
            }
            List<EntrantRawData> allEntrant = getListEntrant(raceUUID, runnerRawData);
            saveEntrant(allEntrant, String.format("%s - %s - %s - %s", raceDto.getMeetingName(), raceDto.getNumber(), raceDto.getRaceType(), date), raceUUID);
            return Flux.fromIterable(allEntrant)
                    .flatMap(r -> Mono.just(EntrantMapper.toEntrantDto(r)));
        } catch (IOException e) {
            throw new ApiRequestFailedException("API request failed: " + e.getMessage(), e);
        }
    }

    public void saveEntrant(List<EntrantRawData> entrantRawData, String raceName, String raceUUID) {
        List<Entrant> newEntrants = entrantRawData.stream().distinct().map(MeetingMapper::toEntrantEntity).collect(Collectors.toList());
        crawUtils.saveEntrantIntoRedis(newEntrants, AppConstant.TAB_SITE_ID, raceName, raceUUID);
    }

    private List<EntrantRawData> getListEntrant(String raceId, TabRunnerRawData runnerRawData) {
        return runnerRawData.getRunners().stream().filter(
                        f -> f.getFixedOdds() != null
                                && f.getFixedOdds().getFlucs() != null
                                && !f.getFixedOdds().getFlucs().isEmpty())
                .map(x -> {
                    List<Float> listPrice = x.getFixedOdds().getFlucs().stream().map(f -> f.getReturnWin() == null ? 0f : f.getReturnWin())
                            .collect(Collectors.toList());
                    return EntrantMapper.toEntrantRawData(x, runnerRawData.getResults(), listPrice, raceId);
                }).collect(Collectors.toList());
    }

    public TabRunnerRawData crawlRunnerDataTAB(String raceId) throws IOException {
        String url = AppConstant.TAB_BET_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
        Response response = ApiUtils.get(url);
        JsonObject jsonObject = JsonParser.parseString(response.body().string()).getAsJsonObject();
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(jsonObject, TabRunnerRawData.class);
    }
}
