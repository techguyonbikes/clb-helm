package com.tvf.clb.service.repository;

import com.tvf.clb.base.entity.Race;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RaceRepository extends R2dbcRepository<Race, Long> {
    Mono<Race> findRaceByRaceId(UUID raceId);
}
