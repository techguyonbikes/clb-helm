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
    Flux<Race> findAllByActualStartAfter(Instant time);
    Flux<Race> findAllByRaceIdIn(List<String> raceIds);
    Flux<Race> findAllByIdIn(List<Long> raceIds);

    @Query("Update clb_db.race set distance =:distance , status =:status  WHERE race_id =:raceId")
    Mono<Race> setUpdateRaceByRaceId(@Param("raceId") String raceId, @Param("distance") Integer distance, @Param("status") String status);

    @Query("SELECT r from race r JOIN race_site rs on r.id = rs.general_race_id " +
            "WHERE rs.race_site_id = :raceId")
    Mono<Race> getRaceByRaceSiteId(@Param("raceId") String raceId);

    @Query("select r.race_id  from clb_db.race r where r.id =:raceId")
    Flux<String> getAllByRaceId(@Param("raceId") Long raceId);

    @Query("select r.id from clb_db.race r  where r.meeting_id = :metingId and r.number = :number and r.advertised_start = :date")
    Mono<Long> getRaceId(@Param("metingId") String metingId, @Param("number") int number, @Param("date") Instant date);
}
