package com.tvf.clb.service.service;

import com.tvf.clb.base.dto.EntrantDto;
import com.tvf.clb.base.dto.MeetingDto;
import com.tvf.clb.base.dto.RaceDto;
import com.tvf.clb.base.exception.ApiRequestFailedException;
import com.tvf.clb.base.model.CrawlRaceData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ICrawlService {

    Logger log = LoggerFactory.getLogger(ICrawlService.class);

    Flux<MeetingDto> getTodayMeetings(LocalDate date);

    Mono<CrawlRaceData> getEntrantByRaceUUID(String raceId);

    default void setRaceIdThenCrawlAndSaveEntrants(Mono<Map<String, Long>> mapUUIDToRaceIdMono, List<RaceDto> raceDtoList, LocalDate date) {
        mapUUIDToRaceIdMono.subscribe(mapUUIDToRaceId -> {
            raceDtoList.forEach(raceDto -> raceDto.setRaceId(mapUUIDToRaceId.get(raceDto.getId())));
            crawlAndSaveEntrants(raceDtoList, date).subscribe();
        });
    }

    default Flux<EntrantDto> crawlAndSaveEntrants(List<RaceDto> raceDtoList, LocalDate date) {
        return Flux.fromIterable(raceDtoList)
                .flatMap(raceDto -> crawlAndSaveEntrantsInRace(raceDto, date).onErrorMap(throwable -> {
                    if (! (throwable instanceof ApiRequestFailedException)) {
                        return new RuntimeException(String.format("Got exception \"%s\" when crawling race uuid = %s, url = %s", throwable.getMessage(), raceDto.getId(), raceDto.getRaceSiteUrl()));
                    }
                    return throwable;
                }))
                .onErrorContinue((throwable, o) -> {
                    if (! (throwable instanceof ApiRequestFailedException)) {
                        log.error(throwable.getMessage());
                    }
                });
    }

    default Flux<EntrantDto> crawlAndSaveEntrantsInRace(RaceDto raceDto, LocalDate date) {
        return Flux.empty();
    }
}
