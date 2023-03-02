package com.tvf.clb.service.repository;

import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Race;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface EntrantRepository extends R2dbcRepository<Entrant, Long> {
    Mono<Entrant> findByEntrantId(String entrantId);
}
