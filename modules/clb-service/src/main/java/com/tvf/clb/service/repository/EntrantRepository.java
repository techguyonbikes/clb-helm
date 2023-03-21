package com.tvf.clb.service.repository;

import com.tvf.clb.base.entity.Entrant;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface EntrantRepository extends R2dbcRepository<Entrant, Long> {
    Flux<Entrant> findAllByRaceId(Long raceId);
    Flux<Entrant> findAllByEntrantIdIn(List<String> entrantIds);

    Flux<Entrant> findAllByIdIn(List<Long> ids);

    Flux<Entrant> findAllByNameInAndNumberInAndBarrierIn(List<String> entrantNames, List<Integer> entrantNumbers, List<Integer> barriers);


    @Query("SELECT e FROM clb_db.entrant e " +
            "JOIN clb_db.race r ON e.race_id = r.id " +
            "WHERE e.name = :name " +
            "AND e.number = :number " +
            "AND e.barrier = :barrier " +
            "AND r.name = :raceName " +
            "AND r.number = :raceNumber ")
    Mono<Entrant> findByNameAndNumberAndBarrier(String name, Integer number, Integer barrier, String raceName, Integer raceNumber);

    Flux<Entrant> getAllByRaceId(Long raceId);
}
