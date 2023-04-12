package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.tvf.clb.base.dto.EntrantResponseDto;
import com.tvf.clb.base.dto.RaceResponseDto;
import com.tvf.clb.base.model.CrawlEntrantData;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.service.repository.EntrantRepository;
import com.tvf.clb.service.repository.RaceRepository;
import io.r2dbc.postgresql.codec.Json;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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

    public Mono<RaceResponseDto> crawlRaceNewDataByRaceId(Long generalRaceId) {
        return raceRedisService.findByRaceId(generalRaceId).flatMap(storedRace ->

                CrawUtils.crawlNewDataByRaceUUID(storedRace.getMapSiteUUID()).doOnNext(raceNewData -> {
                    Map<Integer, CrawlEntrantData> mapEntrants = raceNewData.getMapEntrants();
                    storedRace.setStatus(raceNewData.getStatus());
                    storedRace.getEntrants().forEach(entrant -> {
                        CrawlEntrantData entrantNewData = mapEntrants.get(entrant.getNumber());
                        entrant.setPosition(entrantNewData.getPosition() == null ? 0 : entrantNewData.getPosition());
                        entrant.setPriceFluctuations(entrantNewData.getPriceMap());
                    });

                })
                .then(saveRaceInfoToDBOrRedis(storedRace, generalRaceId))
                .then(Mono.just(storedRace)));
    }

    private Mono<?> saveRaceInfoToDBOrRedis(RaceResponseDto race, Long generalRaceId) {
        if (race.getStatus().equals(AppConstant.STATUS_FINAL)
                || race.getStatus().equals(AppConstant.STATUS_ABANDONED)) {
            log.info("Save race[id={}] data to db and remove in redis", generalRaceId);

            saveEntrantToDb(generalRaceId, race.getEntrants());
            return raceRepository.setUpdateRaceStatusById(generalRaceId, race.getStatus())
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
