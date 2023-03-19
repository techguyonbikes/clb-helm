package com.tvf.clb.service.service;

import com.tvf.clb.base.entity.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.*;

@Service
public class CrawlPriceService {

    private final org.slf4j.Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EntrantRedisService entrantRedisService;

    @Autowired
    private List<ICrawlService> crawlServices;

    public Mono<List<EntrantResponseDto>> crawlPriceByRaceId(Long generalRaceId) {
        Mono<List<EntrantResponseDto>> entrantRedis = entrantRedisService.findEntrantByRaceId(generalRaceId);
        Map<String, Map<Integer, List<Double>>> newPrices = new HashMap<>();
        //todo use crawlSevice to get new price
        entrantRedis.subscribe(x -> {
            String raceUUID = x.stream().map(EntrantResponseDto::getRaceUUID).findFirst().get();
            crawlServices.forEach(service -> {
                Map<String, Map<Integer, List<Double>>> prices = service.getEntrantByRaceId(raceUUID);

            });
        });

        return entrantRedis;
    }

}
