package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.tvf.clb.base.dto.EntrantResponseDto;
import com.tvf.clb.base.dto.RaceResponseDto;
import com.tvf.clb.base.entity.RaceSite;
import com.tvf.clb.base.entity.TodayData;
import com.tvf.clb.base.model.CrawlEntrantData;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.model.CrawlRaceData;
import com.tvf.clb.base.model.PriceHistoryData;
import com.tvf.clb.base.utils.CommonUtils;
import com.tvf.clb.service.repository.EntrantRepository;
import com.tvf.clb.service.repository.RaceRepository;
import com.tvf.clb.service.repository.RaceSiteRepository;
import io.r2dbc.postgresql.codec.Json;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.tvf.clb.base.utils.CommonUtils.setIfPresent;

@Service
public class CrawlPriceService {

    private final org.slf4j.Logger log = LoggerFactory.getLogger(this.getClass());

    private final RaceRedisService raceRedisService;

    private final CrawUtils crawUtils;

    private final EntrantRepository entrantRepository;

    private final RaceRepository raceRepository;

    private final RaceSiteRepository raceSiteRepository;

    private final TodayData todayData;

    public CrawlPriceService(RaceRedisService raceRedisService, CrawUtils crawUtils,
                             EntrantRepository entrantRepository, RaceRepository raceRepository,
                             RaceSiteRepository raceSiteRepository, TodayData todayData) {
        this.raceRedisService = raceRedisService;
        this.crawUtils = crawUtils;
        this.entrantRepository = entrantRepository;
        this.raceRepository = raceRepository;
        this.raceSiteRepository = raceSiteRepository;
        this.todayData = todayData;
    }

    public Mono<?> crawlRaceThenSave(Long generalRaceId) {
        return crawlRaceNewDataByRaceId(generalRaceId)
                .flatMap(this::saveRaceInfoToDBOrRedis);
    }

    private Mono<RaceResponseDto> crawlRaceNewDataByRaceId(Long generalRaceId) {
        return raceRedisService.findByRaceId(generalRaceId).flatMap(storedRace -> {
                    if (storedRace == null) {
                        return Mono.empty();
                    }
                    return updateRaceUUIDAndURLWithSizeUUIDLessThan6(generalRaceId, storedRace)
                            .flatMap(map -> crawUtils.crawlNewDataByRaceUUID(map.getMapSiteUUID())
                                    .map(raceNewData -> {
                                        updateRaceStatusAndFinalResult(map, raceNewData);
                                        updateEntrantsInRace(map.getEntrants(), raceNewData.getMapEntrants());
                                        return map;
                                    }));
                }
        );
    }

    private Mono<RaceResponseDto> updateRaceUUIDAndURLWithSizeUUIDLessThan6(Long generalRaceId, RaceResponseDto storedRace) {
        if (storedRace == null || storedRace.getMapSiteUUID() == null ) {
            return Mono.empty();
        }
        if (storedRace.getMapSiteUUID().size() == AppConstant.RACE_SIZE) {
            return Mono.just(storedRace);
        }

        Flux<RaceSite> raceSiteFlux = raceSiteRepository.getAllByRaceId(generalRaceId).switchIfEmpty(Flux.empty());

        return raceSiteFlux.collectMap(RaceSite::getSiteId, RaceSite::getRaceSiteId)
                .zipWith(raceSiteFlux.collectMap(RaceSite::getSiteId, RaceSite::getRaceSiteUrl))
                .map(tuple -> {
                    if (!CollectionUtils.isEmpty(tuple.getT1()) && !CollectionUtils.isEmpty(tuple.getT2())) {
                        storedRace.setMapSiteUUID(tuple.getT1());
                        storedRace.setRaceSiteUrl(tuple.getT2());
                    }
                    return storedRace;
                });
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
                setIfPresent(entrantNewData.getPosition(), storedEntrant::setPosition);

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
            if (!CollectionUtils.isEmpty(newPrice)) {
                // get data price old of list entrant by siteId
                List<PriceHistoryData> storePriceHistoryData = storedEntrant.getPriceFluctuations().getOrDefault(siteId, new ArrayList<>());
                if (CollectionUtils.isEmpty(storePriceHistoryData)) {
                    storedEntrant.getPriceFluctuations().put(siteId,
                            newPrice.stream().map(x -> new PriceHistoryData(x, CommonUtils.getStringInstantDateNow())).collect(Collectors.toList()));
                } else {
                    //get last price in new list price
                    getLastPriceInListNewPrice(newPrice, storePriceHistoryData, storedEntrant, siteId);

                }
            }
        });
    }

    public void getLastPriceInListNewPrice(List<Float> newPrice, List<PriceHistoryData> storePriceHistoryData, EntrantResponseDto storedEntrant, Integer siteId){
        if (CollectionUtils.isEmpty(newPrice) || CollectionUtils.isEmpty(storePriceHistoryData) || storedEntrant == null || siteId == null ){
            return;
        }

        Float newPriceValue = newPrice.get(newPrice.size() - 1);
        if (!Objects.equals(storePriceHistoryData.get(storePriceHistoryData.size() - 1).getPrice(), newPriceValue)) {
            storePriceHistoryData.add(new PriceHistoryData(newPriceValue, CommonUtils.getStringInstantDateNow()));
        }
        int storeSize = storePriceHistoryData.size();
        if (storeSize > 11) {
            List<PriceHistoryData> newStorePriceHistoryData = new ArrayList<>();
            newStorePriceHistoryData.add(storePriceHistoryData.get(0));

            for (int i = storeSize - 10; i < storeSize; i++) {
                newStorePriceHistoryData.add(storePriceHistoryData.get(i));
            }
            storedEntrant.getPriceFluctuations().put(siteId, newStorePriceHistoryData);

        }
    }

    private Mono<?> saveRaceInfoToDBOrRedis(RaceResponseDto race) {
        long generalRaceId = race.getId();

        if (CommonUtils.isRaceFinalOrAbandonedInAllSite(race)) {
            log.info("Save race[id={}] data to db and remove in redis", generalRaceId);

            Long advertisedStart = Timestamp.from(Instant.parse(race.getAdvertisedStart())).getTime();
            todayData.deleteFinalOrAbandonedRace(advertisedStart, generalRaceId);

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
