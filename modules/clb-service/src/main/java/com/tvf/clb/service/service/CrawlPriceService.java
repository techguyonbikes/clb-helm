package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.tvf.clb.base.dto.EntrantResponseDto;
import com.tvf.clb.base.dto.RaceResponseDto;
import com.tvf.clb.base.entity.TodayData;
import com.tvf.clb.base.model.CrawlEntrantData;
import com.tvf.clb.base.model.CrawlRaceData;
import com.tvf.clb.base.utils.CommonUtils;
import com.tvf.clb.service.repository.EntrantRepository;
import com.tvf.clb.service.repository.RaceRepository;
import io.r2dbc.postgresql.codec.Json;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CrawlPriceService {

    private final org.slf4j.Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RaceRedisService raceRedisService;

    @Autowired
    private CrawUtils CrawUtils;

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

                CrawUtils.crawlNewDataByRaceUUID(storedRace.getMapSiteUUID())
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

        // Do not update if new status is null
        if (newStatus != null) {
            storedRace.setStatus(newStatus);
        }

        if (storedRace.getFinalResult() == null) {
            storedRace.setFinalResult(new HashMap<>());
        }
        storedRace.getFinalResult().putAll(finalResult);
    }

    private void updateEntrantsInRace(List<EntrantResponseDto> storedEntrantList, Map<Integer, CrawlEntrantData> mapNewEntrants) {

        for (EntrantResponseDto storedEntrant : storedEntrantList) {
            CrawlEntrantData entrantNewData = mapNewEntrants.get(storedEntrant.getNumber());

            if (entrantNewData != null) {
                // update entrant position
                if (entrantNewData.getPosition() != null) {
                    storedEntrant.setPosition(entrantNewData.getPosition());
                }

                // update entrant price
                if (storedEntrant.getPriceFluctuations() == null) {
                    storedEntrant.setPriceFluctuations(new HashMap<>());
                }
                Map<Integer, List<Float>> newPriceMap = entrantNewData.getPriceMap();
                newPriceMap.forEach((siteId, newPrice) -> storedEntrant.getPriceFluctuations().put(siteId, newPrice));
            }
        }
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
