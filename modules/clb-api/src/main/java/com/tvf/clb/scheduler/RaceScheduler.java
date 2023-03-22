package com.tvf.clb.scheduler;

import com.tvf.clb.base.entity.Race;
import com.tvf.clb.service.repository.RaceRepository;
import com.tvf.clb.service.service.CrawlPriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@Slf4j
public class RaceScheduler {

    @Autowired
    private RaceRepository raceRepository;

    @Autowired
    private CrawlPriceService crawlPriceService;

    /**
     * Crawling race data start in 1 hours - every 30 seconds.
     */
    @Scheduled(cron = "*/30 * * * * *")
    public void racesDataCrawlingEachMinuteForRaceStartIn() {

        log.info("Start crawl race data start in 1 hours.");

        long startTime = System.currentTimeMillis();
        Instant currentDateTime = Instant.now();
        Flux<Race> races = raceRepository.findAllByActualStartBetween(currentDateTime, currentDateTime.plus(1, ChronoUnit.HOURS));

        races.parallel()
             .runOn(Schedulers.parallel())
             .flatMap(race -> {
                 log.info("Crawl data race id = {} start at {}", race.getId(), race.getActualStart());
                 return crawlPriceService.crawlPriceByRaceId(race.getId());
             })
             .sequential()
             .doFinally(signalType -> log.info("------ All races start in 1 hours are updated, time taken: {} millisecond---------", System.currentTimeMillis() - startTime))
             .then(races.count())
             .subscribe(numberOfRacesNeedToUpdate -> log.info("Number of races just updated: {}", numberOfRacesNeedToUpdate));
    }

    /**
     * Crawling race data start after 1 hours - every 5 minutes.
     */
    @Scheduled(cron = "0 */5 * ? * *")
    public void racesDataCrawlingEach5Minute() {
        log.info("Start crawl race data start after 1 hours.");
        long startTime = System.currentTimeMillis();

        Instant currentDateTime = Instant.now();
        Flux<Race> races = raceRepository.findAllByActualStartAfter(currentDateTime.plus(1, ChronoUnit.HOURS));

        races.parallel()
                .runOn(Schedulers.parallel())
                .flatMap(race -> {
                    log.info("Crawl data race id = {} start at {}", race.getId(), race.getActualStart());
                    return crawlPriceService.crawlPriceByRaceId(race.getId());
                })
                .sequential()
                .doFinally(signalType -> log.info("------ All races start after 1 hours are updated, time taken: {} millisecond---------", System.currentTimeMillis() - startTime))
                .then(races.count())
                .subscribe(numberOfRacesNeedToUpdate -> log.info("Number of races just updated: {}", numberOfRacesNeedToUpdate));
    }

}
