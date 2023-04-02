package com.tvf.clb.service.repository;

import com.tvf.clb.base.entity.Entrant;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
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

    @Query("SELECT e.* FROM clb_db.entrant e" +
            "\nJOIN clb_db.race r ON r.id = e.race_id" +
            "\nJOIN clb_db.meeting m ON r.meeting_id = m.id" +
            "\nWHERE m.name = :meetingName AND m.race_type = :raceType AND r.number = :raceNumber AND advertised_start = :advertisedStart")
    Flux<Entrant> findAllEntrantsInRace(String meetingName, String raceType, Integer raceNumber, Instant advertisedStart);
}
