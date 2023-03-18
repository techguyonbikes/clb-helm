package com.tvf.clb.scheduler;

import com.tvf.clb.base.entity.Race;
import com.tvf.clb.service.repository.RaceRepository;
import com.tvf.clb.service.service.CrawlService;
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
    private CrawlService crawlService;
    @Autowired
    private RaceRepository raceRepository;

//    /**
//     * Crawling race data start in 2 hours - every minute.
//     */
//    @Scheduled(cron = "0 * * ? * *")
//    public void racesDataCrawlingEachMinuteForRaceStartIn() {
//        Instant currentDateTime = Instant.now();
//        Flux<Race> races = raceRepository.findAllByActualStartBetween(currentDateTime, currentDateTime.plus(2, ChronoUnit.HOURS));
//        log.info("Start crawl race data start in 2 hours.");
//        races.publishOn(Schedulers.boundedElastic()).doOnNext(race -> {
//            log.info("Start Crawl Data from v2/racing/?method=racecard&id=" + race.getRaceId());
//            crawlService.getEntrantByRaceId(race.getRaceId()).subscribe();
//        }).subscribe();
//        log.info("---------------------------------------------------");
//    }
//
//    /**
//     * Crawling race data start after 2 hours - every 5 minutes.
//     */
//    @Scheduled(cron = "0 */5 * ? * *")
//    public void racesDataCrawlingEach5Minute() {
//        Instant currentDateTime = Instant.now();
//        Flux<Race> races = raceRepository.findAllByActualStartAfter(currentDateTime.plus(2, ChronoUnit.HOURS));
//        log.info("Start crawl race data start after 2 hours.");
//        races.publishOn(Schedulers.boundedElastic()).doOnNext(race -> {
//            log.info("Start Crawl Data from v2/racing/?method=racecard&id=" + race.getRaceId());
//            crawlService.getEntrantByRaceId(race.getRaceId()).subscribe();
//        }).subscribe();
//        log.info("---------------------------------------------------");
//    }

}
