package com.tvf.clb.service.repository;

import com.tvf.clb.base.entity.EntrantSite;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;

@Repository
public interface EntrantSiteRepository extends R2dbcRepository<EntrantSite, Long> {
}
