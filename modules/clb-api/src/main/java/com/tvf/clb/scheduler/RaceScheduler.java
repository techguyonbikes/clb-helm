package com.tvf.clb.scheduler;

import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.entity.TodayData;
import com.tvf.clb.service.repository.RaceRepository;
import com.tvf.clb.service.service.CrawlPriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

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
     * Crawling race data start in 1 hour - every 30 seconds.
     */
    @Scheduled(cron = "*/30 * * * * *")
    public void crawlRaceDataStartIn1Hour() {

        log.info("Start crawl race data start in 1 hour.");
        long startTime = System.currentTimeMillis();

        Flux<Race> races = getTodayNonFinalAndAbandonedRaceStartBefore(Instant.now().plus(1, ChronoUnit.HOURS));

        races.parallel()
             .runOn(Schedulers.parallel())
             .flatMap(race -> {
                 log.info("Crawl data race id = {} start at {}", race.getId(), race.getActualStart());
                 return crawlPriceService.crawlRaceNewDataByRaceId(race.getId());
             })
             .doOnNext(raceResponseDto -> {
                 if (raceResponseDto.getStatus().equals(STATUS_FINAL) || raceResponseDto.getStatus().equals(STATUS_ABANDONED)) {
                    todayData.removeRaceById(raceResponseDto.getId());
                    log.info("Race id = {} is completed or abandoned, so remove from todayRaces", raceResponseDto.getId());
                 }
             })
             .sequential()
             .doFinally(signalType -> log.info("------ All races start in 1 hour are updated, time taken: {} millisecond---------", System.currentTimeMillis() - startTime))
             .then(races.count())
             .subscribe(numberOfRacesNeedToUpdate -> log.info("Number of races just updated: {}", numberOfRacesNeedToUpdate));
    }

    /**
     * Crawling race data start after 1 hour - every 5 minutes.
     */
    @Scheduled(cron = "0 */5 * ? * *")
    public void crawlRaceDataStartAfter1Hour() {

        log.info("Start crawl race data start after 1 hour.");
        long startTime = System.currentTimeMillis();

        Flux<Race> races = getTodayNonFinalAndAbandonedRaceStartAfter(Instant.now().plus(1, ChronoUnit.HOURS));

        races.parallel()
                .runOn(Schedulers.parallel())
                .flatMap(race -> {
                    log.info("Crawl data race id = {} start at {}", race.getId(), race.getActualStart());
                    return crawlPriceService.crawlRaceNewDataByRaceId(race.getId());
                })
                .sequential()
                .doFinally(signalType -> log.info("------ All races start after 1 hour are updated, time taken: {} millisecond---------", System.currentTimeMillis() - startTime))
                .then(races.count())
                .subscribe(numberOfRacesNeedToUpdate -> log.info("Number of races just updated: {}", numberOfRacesNeedToUpdate));
    }

    public Flux<Race> getTodayNonFinalAndAbandonedRaceStartBefore(Instant time) {
        return getTodayNonFinalAndAbandonedRace()
                .filter(race -> race.getAdvertisedStart().isBefore(time) && !race.getStatus().equals(STATUS_FINAL) && !race.getStatus().equals(STATUS_ABANDONED));
    }

    public Flux<Race> getTodayNonFinalAndAbandonedRaceStartAfter(Instant time) {
        return getTodayNonFinalAndAbandonedRace()
                .filter(race -> race.getAdvertisedStart().isAfter(time) && !race.getStatus().equals(STATUS_FINAL) && !race.getStatus().equals(STATUS_ABANDONED));
    }

    public Flux<Race> getTodayNonFinalAndAbandonedRace() {
        if (todayData.getRaces() == null) {
            log.info("TodayRaces has no data so need to search in DB");
            todayData.setRaces(new ConcurrentHashMap<>());
            Instant startTime = Instant.now().atZone(ZoneOffset.UTC).with(LocalTime.MIN).minusHours(3).toInstant();
            Instant endOfToday = Instant.now().atZone(ZoneOffset.UTC).with(LocalTime.MAX).toInstant();
            return raceRepository.findAllByAdvertisedStartBetweenAndStatusNotIn(startTime, endOfToday, Arrays.asList(STATUS_FINAL, STATUS_ABANDONED))
                    .doOnNext(race -> todayData.addRace(race.getId(), race));
        } else {
            log.info("TodayRaces has data so no need to search in DB, map race size = {}", todayData.getRaces().size());
            return Flux.fromIterable(todayData.getRaces().values());
        }

    }
}
