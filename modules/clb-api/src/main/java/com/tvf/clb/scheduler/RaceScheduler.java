package com.tvf.clb.scheduler;

import com.tvf.clb.base.entity.TodayData;
import com.tvf.clb.service.repository.RaceRepository;
import com.tvf.clb.service.service.CrawlPriceService;
import com.tvf.clb.socket.socket.SocketModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

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

    @Autowired
    private SocketModule socketModule;

    private boolean isCrawlingRaceStartIn30Minutes = false;
    private boolean isCrawlingRaceStartAfter30MinutesAndIn1Hour = false;
    private boolean isCrawlingRaceStartAfter1Hour = false;
    private boolean isCrawlingSubscriRaceStart = false;

    @Scheduled(cron = "*/4 * * * * *")
    public void crawlSubscribedRace() {

        long startTime = System.currentTimeMillis();

        if (isCrawlingSubscriRaceStart || CollectionUtils.isEmpty(socketModule.getRaceSubscribers().keySet())) {
            return;
        }

        isCrawlingSubscriRaceStart = true;

        log.info("Start crawl subscribed race data.");

        Flux<Long> raceIds = Flux.fromIterable(socketModule.getSubscribedRaces().keySet());

        raceIds
            .flatMap(raceId -> {
                log.info("Crawl data subscribed race id = {}", raceId);
                return crawlPriceService.crawlRaceThenSave(raceId);
            })
            .doFinally(signalType -> {
                log.info("------ All subscribed races are updated, time taken: {} millisecond---------", System.currentTimeMillis() - startTime);
                isCrawlingSubscriRaceStart = false;
            })
            .then(raceIds.count())
            .subscribe(numberOfRacesNeedToUpdate -> log.info("Number of subscribed races just updated: {}", numberOfRacesNeedToUpdate));
    }

    /**
     * Crawling race data start in 30 minutes - every 10 seconds.
     */
    @Scheduled(cron = "*/10 * * * * *")
    public void crawlRaceDataStartIn30Minutes() {

        if (isCrawlingRaceStartIn30Minutes || todayData.getLastTimeCrawl().plus(10, ChronoUnit.MINUTES).isAfter(Instant.now())) {
            return;
        }

        log.info("Start crawl race data start in 30 minutes.");
        isCrawlingRaceStartIn30Minutes = true;
        long startTime = System.currentTimeMillis();

        Long raceStartTimeFrom = Instant.now().atZone(ZoneOffset.UTC).with(LocalTime.MIN).minus(2, ChronoUnit.HOURS).toInstant().toEpochMilli();
        Long raceStartTimeTo = Instant.now().plus(30, ChronoUnit.MINUTES).toEpochMilli();

        Flux<Long> raceIds = getAllRaceIdsStartBetween(raceStartTimeFrom, raceStartTimeTo);


        raceIds
            .flatMap(raceId -> {
                log.info("Crawl data race id = {} start in 30 minutes", raceId);
                return crawlPriceService.crawlRaceThenSave(raceId);
            })
            .doFinally(signalType -> {
                log.info("------ All races start in 30 minutes are updated, time taken: {} millisecond---------", System.currentTimeMillis() - startTime);
                isCrawlingRaceStartIn30Minutes = false;
            })
            .then(raceIds.count())
            .subscribe(numberOfRacesNeedToUpdate -> log.info("Number of races start in 30 minutes just updated: {}", numberOfRacesNeedToUpdate));
    }

    /**
     * Crawling race data start after 30 minutes and in 1 hour - every 15 seconds.
     */
    @Scheduled(cron = "*/15 * * * * *")
    public void crawlRaceDataStartAfter30MinutesAndIn1Hour() {

        if (isCrawlingRaceStartAfter30MinutesAndIn1Hour || todayData.getLastTimeCrawl().plus(10, ChronoUnit.MINUTES).isAfter(Instant.now())) {
            return;
        }

        log.info("Start crawl race data start after 30 minutes and in 1 hour.");
        isCrawlingRaceStartAfter30MinutesAndIn1Hour = true;
        long startTime = System.currentTimeMillis();


        Long raceStartTimeFrom = Instant.now().plus(30, ChronoUnit.MINUTES).toEpochMilli();
        Long raceStartTimeTo = Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli();

        Flux<Long> raceIds = getAllRaceIdsStartBetween(raceStartTimeFrom, raceStartTimeTo);

        raceIds
            .flatMap(raceId -> {
                log.info("Crawl data race id = {} start after 30 minutes and in 1 hour", raceId);
                return crawlPriceService.crawlRaceThenSave(raceId);
            })
            .doFinally(signalType -> {
                log.info("------ All races start after 30 minutes and in 1 hour are updated, time taken: {} millisecond---------", System.currentTimeMillis() - startTime);
                isCrawlingRaceStartAfter30MinutesAndIn1Hour = false;
            })
            .then(raceIds.count())
            .subscribe(numberOfRacesNeedToUpdate -> log.info("Number of races start after 30 minutes and in 1 hour just updated: {}", numberOfRacesNeedToUpdate));
    }

    /**
     * Crawling race data start after 1 hour - every 5 minutes.
     */
    @Scheduled(cron = "0 */5 * ? * *")
    public void crawlRaceDataStartAfter1Hour() {

        if (isCrawlingRaceStartAfter1Hour || todayData.getLastTimeCrawl().plus(10, ChronoUnit.MINUTES).isAfter(Instant.now())) {
            return;
        }

        log.info("Start crawl race data start after 1 hour.");
        isCrawlingRaceStartAfter1Hour = true;
        long startTime = System.currentTimeMillis();

        Long raceStartTimeFrom = Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli();
        Long raceStartTimeTo = Instant.now().atZone(ZoneOffset.UTC).with(LocalTime.MAX).toInstant().toEpochMilli();

        Flux<Long> raceIds = getAllRaceIdsStartBetween(raceStartTimeFrom, raceStartTimeTo);

        raceIds
            .flatMap(raceId -> {
                log.info("Crawl data race id = {} start after 1 hour", raceId);
                return crawlPriceService.crawlRaceThenSave(raceId);
            })
            .doFinally(signalType -> {
                log.info("------ All races start after 1 hour are updated, time taken: {} millisecond---------", System.currentTimeMillis() - startTime);
                isCrawlingRaceStartAfter1Hour = false;
            })
            .then(raceIds.count())
            .subscribe(numberOfRacesNeedToUpdate -> log.info("Number of races start after 1 hour just updated: {}", numberOfRacesNeedToUpdate));
    }

    public Flux<Long> getAllRaceIdsStartBetween(Long raceStartTimeFrom, Long raceStartTimeTo) {
        return getTodayNonFinalAndAbandonedRace()
                .map(treeMap -> treeMap.subMap(raceStartTimeFrom, raceStartTimeTo))
                .flatMapMany(treeMap ->
                        Flux.fromIterable(treeMap.values().stream().flatMap(Collection::stream).filter(raceId -> !socketModule.getSubscribedRaces().containsKey(raceId)).collect(Collectors.toList())));
    }

    public Mono<TreeMap<Long, List<Long>>> getTodayNonFinalAndAbandonedRace() {
        if (todayData.getRaces() == null) {
            log.info("TodayRaces has no data so need to search in DB");
            todayData.setRaces(new TreeMap<>());
            Instant startTime = Instant.now().atZone(ZoneOffset.UTC).with(LocalTime.MIN).toInstant();
            Instant endOfToday = Instant.now().atZone(ZoneOffset.UTC).with(LocalTime.MAX).toInstant();

            return raceRepository.findAllByAdvertisedStartBetweenAndStatusNotIn(startTime, endOfToday, Arrays.asList(STATUS_FINAL, STATUS_ABANDONED))
                    .doOnNext(race -> todayData.addOrUpdateRace(race.getAdvertisedStart().toEpochMilli(), race.getId()))
                    .then(Mono.just(todayData.getRaces()));
        } else {
            log.info("TodayRaces has data so no need to search in DB, map race size = {}", todayData.getRaces().size());
            return Mono.just(todayData.getRaces());
        }

    }
}
