package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.tvf.clb.base.dto.EntrantMapper;
import com.tvf.clb.base.entity.EntrantResponseDto;
import com.tvf.clb.base.model.CrawlEntrantData;
import com.tvf.clb.service.repository.EntrantRepository;
import io.r2dbc.postgresql.codec.Json;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;

@Service
public class CrawlPriceService {

    private final org.slf4j.Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EntrantRedisService entrantRedisService;

    @Autowired
    private CrawUtils CrawUtils;

    @Autowired
    private EntrantRepository entrantRepository;

    private Gson gson = new Gson();

    public Mono<List<EntrantResponseDto>> crawlEntrantPricePositionByRaceId(Long generalRaceId) {
        return entrantRedisService.findEntrantByRaceId(generalRaceId).flatMap(x -> {
            List<EntrantResponseDto> storeRecords = EntrantMapper.convertFromRedisPriceToDTO(x);

            return CrawUtils.crawlNewPriceByRaceUUID(storeRecords.get(0).getRaceUUID()).doOnNext(newPrices -> {
                storeRecords.forEach(entrant -> {
                            CrawlEntrantData entrantData = newPrices.get(entrant.getNumber());
                            entrant.setPriceFluctuations(entrantData.getPriceMap());
                            entrant.setPosition(entrantData.getPosition() == null ? 0 : entrantData.getPosition());
                        }
                );

                if (storeRecords.stream().anyMatch(storeRecord -> storeRecord.getPosition() > 0)) {
                    log.info("-------- Save entrant price to db and remove data in redis: {}", generalRaceId);
                    // save to db and remove data in redis
                    saveEntrantToDb(generalRaceId, storeRecords);
                    entrantRedisService.delete(generalRaceId).subscribe();
                } else {
                    log.info("-------- Save entrant price to redis: {}", generalRaceId);
                    entrantRedisService.saveRace(generalRaceId, storeRecords).subscribe();
                }

            }).then(Mono.just(storeRecords));
        });
    }

    public void saveEntrantToDb(Long generalRaceId, List<EntrantResponseDto> storeRecords) {
        entrantRepository.getAllByRaceId(generalRaceId).collectList().subscribe(existed -> {
            storeRecords.forEach(e ->
            {
                existed.stream()
                        .filter(x -> x.getName().equals(e.getName())
                                && x.getNumber().equals(e.getNumber())
                        )
                        .findFirst()
                        .ifPresent(entrant -> {
                                    entrant.setPriceFluctuations(Json.of(gson.toJson(e.getPriceFluctuations() == null ? new HashMap<>() : e.getPriceFluctuations())));
                                    entrant.setPosition(e.getPosition());
                                }

                        );
            });
            entrantRepository.saveAll(existed).subscribe();
        });
    }

}
