package com.tvf.clb.service.repository;

import com.tvf.clb.base.entity.RaceSite;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;

@Repository
public interface RaceSiteRepository extends R2dbcRepository<RaceSite, Long> {

    Flux<RaceSite> findAllByRaceSiteIdInAndSiteId(List<String> raceId, Integer siteId);

    Flux<RaceSite> findAllByRaceSiteIdIn(List<String> raceId);

    Flux<RaceSite> getAllByGeneralRaceId(Long generalRaceId);

}
