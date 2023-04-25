package com.tvf.clb.service.repository;

import com.tvf.clb.base.entity.Entrant;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

public interface EntrantRepository extends R2dbcRepository<Entrant, Long> {

    Flux<Entrant> findAllByNameInAndNumberInAndBarrierIn(List<String> entrantNames, List<Integer> entrantNumbers, List<Integer> barriers);


    Flux<Entrant> getAllByRaceId(Long raceId);

    Flux<Entrant> findByRaceId(Long raceId);

    @Query("delete from clb_db.entrant e where e.race_id in (select r.id from clb_db.race r where r.advertised_start between :startTime and :endTime)")
    Mono<Long> deleteAllByAdvertisedStartBetween(@Param("startTime") Instant  startTime, @Param("endTime") Instant endTime);
}
