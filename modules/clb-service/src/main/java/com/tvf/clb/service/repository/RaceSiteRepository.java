package com.tvf.clb.service.repository;

import com.tvf.clb.base.entity.RaceSite;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Repository
public interface RaceSiteRepository extends R2dbcRepository<RaceSite, Long> {

    Flux<RaceSite> findAllByGeneralRaceIdInAndSiteId(List<Long> raceId, Integer siteId);

    Flux<RaceSite> getAllByGeneralRaceId(Long generalRaceId);

    @Query("delete from clb_db.race_site r where r.start_date between :startTime and :endTime")
    Mono<Long> deleteAllByStartDateBetween(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    @Query("Update clb_db.race_site set race_site_id = :uuid where id = :id")
    Mono<Boolean> updateRaceSiteId(String uuid, Long id);

}
