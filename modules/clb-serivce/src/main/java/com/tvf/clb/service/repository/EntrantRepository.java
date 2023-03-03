package com.tvf.clb.service.repository;

import com.tvf.clb.base.entity.Entrant;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import java.util.UUID;

public interface EntrantRepository extends R2dbcRepository<Entrant, UUID> {

}
