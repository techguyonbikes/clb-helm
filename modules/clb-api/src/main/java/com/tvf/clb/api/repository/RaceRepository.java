package com.tvf.clb.api.repository;

import com.tvf.clb.base.entity.Race;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

public interface RaceRepository extends R2dbcRepository<Race, UUID> {
}
