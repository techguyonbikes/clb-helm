package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.tvf.clb.base.dto.EntrantResponseDto;
import com.tvf.clb.base.dto.RaceResponseDto;
import com.tvf.clb.base.entity.TodayData;
import com.tvf.clb.base.model.CrawlEntrantData;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.model.CrawlRaceData;
import com.tvf.clb.base.model.PriceHistoryData;
import com.tvf.clb.base.utils.CommonUtils;
import com.tvf.clb.service.repository.EntrantRepository;
import com.tvf.clb.service.repository.RaceRepository;
import io.r2dbc.postgresql.codec.Json;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CrawlPriceService {

    private final org.slf4j.Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RaceRedisService raceRedisService;

    @Autowired
    private CrawUtils crawUtils;

    @Autowired
    private EntrantRepository entrantRepository;

    @Autowired
    private RaceRepository raceRepository;

    @Autowired
    private TodayData todayData;

    public Mono<?> crawlRaceThenSave(Long generalRaceId) {
        return crawlRaceNewDataByRaceId(generalRaceId)
                .flatMap(this::saveRaceInfoToDBOrRedis);
    }

    private Mono<RaceResponseDto> crawlRaceNewDataByRaceId(Long generalRaceId) {
        return raceRedisService.findByRaceId(generalRaceId).flatMap(storedRace ->

                crawUtils.crawlNewDataByRaceUUID(storedRace.getMapSiteUUID())
                        .map(raceNewData -> {
                            updateRaceStatusAndFinalResult(storedRace, raceNewData);
                            updateEntrantsInRace(storedRace.getEntrants(), raceNewData.getMapEntrants());

                            return storedRace;
                        })
        );
    }

    private void updateRaceStatusAndFinalResult(RaceResponseDto storedRace, CrawlRaceData raceNewData) {

        String newStatus = raceNewData.getStatus();
        Map<Integer, String> finalResult = raceNewData.getFinalResult();
        Map<Integer, String> interimResult = raceNewData.getInterimResult();

        if (storedRace.getFinalResult() == null) {
            storedRace.setFinalResult(new HashMap<>());
        }
        storedRace.getFinalResult().putAll(finalResult);

        if (storedRace.getInterimResult() == null) {
            storedRace.setInterimResult(new HashMap<>());
        }
        storedRace.getInterimResult().putAll(interimResult);

        // Do not update if new status is null
        if (newStatus != null) {
            String zBetFinalResult = storedRace.getFinalResult().get(AppConstant.ZBET_SITE_ID);
            String zBetInterimResult = storedRace.getInterimResult().get(AppConstant.ZBET_SITE_ID);

            if (zBetFinalResult != null && zBetInterimResult != null && ! zBetFinalResult.equals(zBetInterimResult)) {
                newStatus = AppConstant.STATUS_RE_RESULTED;
            }
            storedRace.setStatus(newStatus);
        }

    }

    private void updateEntrantsInRace(List<EntrantResponseDto> storedEntrantList, Map<Integer, CrawlEntrantData> mapNewEntrants) {

        for (EntrantResponseDto storedEntrant : storedEntrantList) {
            CrawlEntrantData entrantNewData = mapNewEntrants.get(storedEntrant.getNumber());

            if (entrantNewData != null) {
                // update entrant position
                if (entrantNewData.getPosition() != null) {
                    storedEntrant.setPosition(entrantNewData.getPosition());
                }

                //update scratch
                if (Boolean.TRUE.equals(entrantNewData.getIsScratched())){
                    storedEntrant.setIsScratched(entrantNewData.getIsScratched());
                    storedEntrant.setScratchedTime(entrantNewData.getScratchTime() == null ? null : entrantNewData.getScratchTime().toString());
                }

                // update entrant price
                if (storedEntrant.getPriceFluctuations() == null) {
                    storedEntrant.setPriceFluctuations(new HashMap<>());
                }

                //update price
                updatePriceToRedis(entrantNewData.getPriceMap(), storedEntrant);

            }
        }
    }

    /**
     *
     * @param newPriceMap new price map
     * @param storedEntrant current entrant - old entrant properties
     */
    public void updatePriceToRedis(Map<Integer, List<Float>> newPriceMap, EntrantResponseDto storedEntrant) {
        if (CollectionUtils.isEmpty(newPriceMap) || storedEntrant == null) {
            return;
        }

        // For each new price
        newPriceMap.forEach((siteId, newPrice) -> {
            if (CollectionUtils.isEmpty(newPrice)) {
                newPriceMap.forEach((i, p) -> storedEntrant.getPriceFluctuations().put(i,
                        p.stream().map(x -> new PriceHistoryData(x, CommonUtils.getStringInstantDateNow())).collect(Collectors.toList())));
            } else {
                // get data price old of list entrant by siteId
                List<PriceHistoryData> storePriceHistoryData = storedEntrant.getPriceFluctuations().getOrDefault(siteId, new ArrayList<>());
                if (CollectionUtils.isEmpty(storePriceHistoryData)) {
                    newPriceMap.forEach((i, p) ->
                            storedEntrant.getPriceFluctuations().put(i,
                                    p.stream().map(x -> new PriceHistoryData(x, CommonUtils.getStringInstantDateNow())).collect(Collectors.toList())));
                } else {
                    //get last price in new list price
                    Float newPriceValue = newPrice.get(newPrice.size() - 1);
                    if (!Objects.equals(storePriceHistoryData.get(storePriceHistoryData.size() - 1).getPrice(), newPriceValue)) {
                        storePriceHistoryData.add(new PriceHistoryData(newPriceValue, CommonUtils.getStringInstantDateNow()));
                    }
                    int storeSize = storePriceHistoryData.size();
                    if (storeSize > 11) {
                        List<PriceHistoryData> newStorePriceHistoryData = new ArrayList<>();
                        newStorePriceHistoryData.add(storePriceHistoryData.get(0));
                        newStorePriceHistoryData.add(storePriceHistoryData.get(1));

                        for (int i = storeSize - 11; i < storeSize; i++) {
                            newStorePriceHistoryData.add(storePriceHistoryData.get(i));
                        }
                        storedEntrant.getPriceFluctuations().put(siteId, newStorePriceHistoryData);

                    }

                }
            }
        });
    }

    private Mono<?> saveRaceInfoToDBOrRedis(RaceResponseDto race) {
        long generalRaceId = race.getId();

        if (CommonUtils.isRaceFinalOrAbandonedInAllSite(race)) {
            log.info("Save race[id={}] data to db and remove in redis", generalRaceId);

            todayData.getRaces().remove(Timestamp.from(Instant.parse(race.getAdvertisedStart())).getTime());

            saveEntrantToDb(generalRaceId, race.getEntrants());

            Json raceFinalResult = Json.of(new Gson().toJson(race.getFinalResult()));

            return raceRepository.updateRaceStatusAndFinalResultById(generalRaceId, race.getStatus(), raceFinalResult)
                                 .then(raceRedisService.delete(generalRaceId));
        } else {
            log.info(" Save data race[id={}] to redis", generalRaceId);
            return raceRedisService.saveRace(generalRaceId, race);
        }
    }

    public void saveEntrantToDb(Long generalRaceId, List<EntrantResponseDto> storeRecords) {
        entrantRepository.getAllByRaceId(generalRaceId).collectList().subscribe(existed -> {
            storeRecords.forEach(e ->
                    existed.stream()
                            .filter(x -> x.getName().equals(e.getName())
                                    && x.getNumber().equals(e.getNumber())
                            )
                            .findFirst()
                            .ifPresent(entrant -> {
                                        entrant.setPriceFluctuations(Json.of(new Gson().toJson(e.getPriceFluctuations() == null ? new HashMap<>() : e.getPriceFluctuations())));
                                        entrant.setPosition(e.getPosition());
                                    }

                            ));
            entrantRepository.saveAll(existed).subscribe();
        });
    }

}
