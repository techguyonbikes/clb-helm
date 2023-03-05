package com.tvf.clb.service.repository;

import com.tvf.clb.base.entity.Entrant;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

public interface EntrantRepository extends R2dbcRepository<Entrant, UUID> {
    Flux<Entrant> findByRaceId(String id);

    Flux<Entrant> findAllByEntrantIdIn(List<String> entrantIds);
}
