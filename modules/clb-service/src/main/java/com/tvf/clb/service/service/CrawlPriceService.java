package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tvf.clb.base.entity.EntrantResponseDto;
import com.tvf.clb.service.repository.EntrantRepository;
import io.r2dbc.postgresql.codec.Json;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CrawlPriceService {

    private final org.slf4j.Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EntrantRedisService entrantRedisService;

    @Autowired
    private List<ICrawlService> crawlServices;

    @Autowired
    private EntrantRepository entrantRepository;

    private Gson gson = new Gson();


    public Mono<List<EntrantResponseDto>> crawlPriceByRaceId(Long generalRaceId) {
        Mono<List<EntrantResponseDto>> entrantRedis = entrantRedisService.findEntrantByRaceId(generalRaceId);

        return entrantRedis.flatMap(x -> {

            Type listType = new TypeToken<List<EntrantResponseDto>>() {}.getType();
            List<EntrantResponseDto> storeRecords = gson.fromJson(gson.toJson(x), listType);
            String raceUUID = storeRecords.stream().map(EntrantResponseDto::getRaceUUID).findFirst().get();

            Map<String, Map<Integer, List<Double>>> newPrices = new HashMap<>();


            return Flux.fromIterable(crawlServices)
                    .parallel().runOn(Schedulers.parallel())
                    .map(iCrawlService -> iCrawlService.getEntrantByRaceId(raceUUID))
                    .sequential().doOnNext(prices -> {
                        prices.forEach((key, value) -> {
                            if (newPrices.containsKey(key)) {
                                newPrices.get(key).putAll(value);
                            } else {
                                newPrices.putAll(prices);
                            }
                        });
                    }).doOnComplete(() -> {
                        storeRecords.forEach(entrant -> entrant.setPriceFluctuations(newPrices.get(entrant.getEntrantId())));

                        if (storeRecords.stream().anyMatch(record -> record.getPosition() > 0)) {
                            log.info("-------- Save entrant price to db and remove data in redis: " + generalRaceId);
                            // save to db and remove data in redis
                            saveEntrantToDb(generalRaceId, storeRecords);
                            entrantRedisService.delete(generalRaceId).subscribe();
                        } else {
                            log.info("-------- Save entrant price to redis: " + generalRaceId);
                            entrantRedisService.saveRace(generalRaceId, storeRecords).subscribe();
                        }

                    }).then().then(Mono.just(storeRecords));
        });
    }

    public void saveEntrantToDb(Long generalRaceId, List<EntrantResponseDto> storeRecords) {
        entrantRepository.getAllByRaceId(generalRaceId).collectList().subscribe(existed -> {
            storeRecords.forEach(e ->
            {
                existed.stream()
                        .filter(x -> x.getName().equals(e.getName())
                                && x.getNumber().equals(e.getNumber())
                                && x.getBarrier().equals(e.getBarrier()))
                        .findFirst()
                        .ifPresent(entrant -> entrant.setPriceFluctuations(Json.of(gson.toJson(e.getPriceFluctuations()))));
            });
            entrantRepository.saveAll(existed).subscribe();
        });
    }

}
