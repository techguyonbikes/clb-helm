package com.tvf.clb.service.service;

import com.tvf.clb.base.dto.EntrantDto;
import com.tvf.clb.base.dto.MeetingDto;
import com.tvf.clb.base.dto.RaceDto;
import com.tvf.clb.base.exception.ApiRequestFailedException;
import com.tvf.clb.base.model.CrawlRaceData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.util.List;

public interface ICrawlService {

    Flux<MeetingDto> getTodayMeetings(LocalDate date);

    CrawlRaceData getEntrantByRaceUUID(String raceId);

    default Flux<EntrantDto> crawlAndSaveEntrants(List<RaceDto> raceDtoList, LocalDate date) {
        return Flux.fromIterable(raceDtoList)
                .parallel((int) (Schedulers.DEFAULT_POOL_SIZE * 0.3)) // create a parallel flux
                .runOn(Schedulers.parallel()) // specify which scheduler to use for the parallel execution
                .flatMap(raceDto -> { // call the getRaceById method for each raceId
                    try {
                        return crawlAndSaveEntrantsInRace(raceDto, date);
                    } catch (Exception e) {
                        return Mono.empty();
                    }
                })
                .sequential(); // convert back to a sequential flux
    }

    default Flux<EntrantDto> crawlAndSaveEntrantsInRace(RaceDto raceDto, LocalDate date) {
        return Flux.empty();
    }
}
