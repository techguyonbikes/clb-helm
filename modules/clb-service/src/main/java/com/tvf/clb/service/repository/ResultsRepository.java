package com.tvf.clb.service.repository;

import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Results;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

public interface ResultsRepository extends R2dbcRepository<Results, UUID> {

}
