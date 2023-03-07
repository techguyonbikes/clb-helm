package com.tvf.clb.service.repository;

import com.tvf.clb.base.entity.AdditionalInfo;
import com.tvf.clb.base.entity.Results;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface AdditionalInfoResponse extends R2dbcRepository<AdditionalInfo, UUID> {
    Flux<AdditionalInfo> findByRaceId(String id);
}
