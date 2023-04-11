package com.tvf.clb.scheduler;

import com.tvf.clb.base.entity.TodayData;
import com.tvf.clb.service.repository.RaceRepository;
import com.tvf.clb.service.service.CrawlPriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.TreeMap;

import static com.tvf.clb.base.utils.AppConstant.STATUS_ABANDONED;
import static com.tvf.clb.base.utils.AppConstant.STATUS_FINAL;

@Component
@Slf4j
public class RaceScheduler {

    @Autowired
    private RaceRepository raceRepository;

    @Autowired
    private CrawlPriceService crawlPriceService;

    @Autowired
    private TodayData todayData;

    /**
     * Crawling race data start in 30 minutes - every 15 seconds.
     */
    @Scheduled(cron = "*/15 * * * * *")
    public void crawlRaceDataStartIn1Hour() {

        log.info("Start crawl race data start in 30 minutes.");
        long startTime = System.currentTimeMillis();

        Long raceStartTimeFrom = Timestamp.from(Instant.now().minus(30, ChronoUnit.MINUTES)).getTime();
        Long raceStartTimeTo = Timestamp.from(Instant.now().plus(30, ChronoUnit.MINUTES)).getTime();

        Flux<Long> raceIds = getTodayNonFinalAndAbandonedRace()
                                .map(treeMap -> treeMap.subMap(raceStartTimeFrom, raceStartTimeTo))
                                .flatMapMany(treeMap -> Flux.fromIterable(treeMap.values()));

        raceIds.parallel()
             .runOn(Schedulers.parallel())
             .flatMap(raceId -> {
                 log.info("Crawl data race id = {}", raceId);
                 return crawlPriceService.crawlRaceNewDataByRaceId(raceId);
             })
             .doOnNext(race -> {
                 if (race.getStatus().equals(STATUS_FINAL) || race.getStatus().equals(STATUS_ABANDONED)) {
                     todayData.getRaces().remove(Timestamp.from(Instant.parse(race.getAdvertisedStart())).getTime());
                 }
             })
             .sequential()
             .doFinally(signalType -> log.info("------ All races start in 30 minutes are updated, time taken: {} millisecond---------", System.currentTimeMillis() - startTime))
             .then(raceIds.count())
             .subscribe(numberOfRacesNeedToUpdate -> log.info("Number of races just updated: {}", numberOfRacesNeedToUpdate));
    }

    /**
     * Crawling race data start after 30 minutes - every 5 minutes.
     */
    @Scheduled(cron = "0 */5 * ? * *")
    public void crawlRaceDataStartAfter1Hour() {

        log.info("Start crawl race data start after 30 minutes.");
        long startTime = System.currentTimeMillis();

        Long raceStartTimeFrom = Timestamp.from(Instant.now().plus(30, ChronoUnit.MINUTES)).getTime();

        Flux<Long> raceIds = getTodayNonFinalAndAbandonedRace()
                                .map(treeMap -> treeMap.tailMap(raceStartTimeFrom))
                                .flatMapMany(treeMap -> Flux.fromIterable(treeMap.values()));

        raceIds.parallel()
                .runOn(Schedulers.parallel())
                .flatMap(raceId -> {
                    log.info("Crawl data race id = {}", raceId);
                    return crawlPriceService.crawlRaceNewDataByRaceId(raceId);
                })
                .sequential()
                .doFinally(signalType -> log.info("------ All races start after 30 minutes are updated, time taken: {} millisecond---------", System.currentTimeMillis() - startTime))
                .then(raceIds.count())
                .subscribe(numberOfRacesNeedToUpdate -> log.info("Number of races just updated: {}", numberOfRacesNeedToUpdate));
    }

    public Mono<TreeMap<Long, Long>> getTodayNonFinalAndAbandonedRace() {
        if (todayData.getRaces() == null) {
            log.info("TodayRaces has no data so need to search in DB");
            todayData.setRaces(new TreeMap<>());
            Instant startTime = Instant.now().atZone(ZoneOffset.UTC).with(LocalTime.MIN).minusHours(1).toInstant();
            Instant endOfToday = Instant.now().atZone(ZoneOffset.UTC).with(LocalTime.MAX).toInstant();

            return raceRepository.findAllByAdvertisedStartBetweenAndStatusNotIn(startTime, endOfToday, Arrays.asList(STATUS_FINAL, STATUS_ABANDONED))
                    .doOnNext(race -> todayData.addRace(Timestamp.from(race.getAdvertisedStart()).getTime(), race.getId()))
                    .then(Mono.just(todayData.getRaces()));
        } else {
            log.info("TodayRaces has data so no need to search in DB, map race size = {}", todayData.getRaces().size());
            return Mono.just(todayData.getRaces());
        }

    }
}
