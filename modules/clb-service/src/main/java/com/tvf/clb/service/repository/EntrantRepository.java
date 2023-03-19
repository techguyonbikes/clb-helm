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

    @Query("select e.id from clb_db.entrant e where e.name = :name and e.number = :number and e.barrier = :barrier")
    Mono<Long> getEntrantId(@Param("name") String name, @Param("number") Integer number, @Param("barrier") Integer barrier);
}
