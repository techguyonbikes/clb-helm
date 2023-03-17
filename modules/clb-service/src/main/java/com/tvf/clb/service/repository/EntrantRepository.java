package com.tvf.clb.service.repository;

import com.tvf.clb.base.entity.Entrant;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.List;

public interface EntrantRepository extends R2dbcRepository<Entrant, Long> {
    Flux<Entrant> findByRaceId(String id);
    Flux<Entrant> findAllByEntrantIdIn(List<String> entrantIds);

    Flux<Entrant> findAllByIdIn(List<Long> ids);
}
