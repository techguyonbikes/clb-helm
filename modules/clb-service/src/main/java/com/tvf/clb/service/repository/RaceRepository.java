package com.tvf.clb.service.repository;

import com.tvf.clb.base.entity.Race;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

public interface RaceRepository extends R2dbcRepository<Race, Long> {
    Mono<Race> findRaceByRaceId(String raceId);
    Flux<Race> findAllByActualStartBetween(Instant start, Instant end);
    Flux<Race> findAllByRaceIdIn(List<String> raceIds);
    @Query("Update clb_db.race set distance =:distance , status =:status  WHERE race_id =:raceId")
    Mono<Race> setUpdateRaceByRaceId(@Param("raceId") String raceId, @Param("distance") Integer distance, @Param("status") String status);

}
