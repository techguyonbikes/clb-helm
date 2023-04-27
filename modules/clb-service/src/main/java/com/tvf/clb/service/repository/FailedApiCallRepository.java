package com.tvf.clb.service.repository;

import com.tvf.clb.base.entity.FailedApiCall;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Repository
public interface FailedApiCallRepository extends R2dbcRepository<FailedApiCall, Long> {

    Mono<FailedApiCall> findByClassNameAndMethodNameAndParams(String className, String methodName, String param);

    Flux<FailedApiCall> findByFailedTimeBetween(Instant startTime, Instant endTime);
}
