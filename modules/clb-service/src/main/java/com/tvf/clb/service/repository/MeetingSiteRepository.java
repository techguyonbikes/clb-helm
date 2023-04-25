package com.tvf.clb.service.repository;

import com.tvf.clb.base.entity.MeetingSite;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Repository
public interface MeetingSiteRepository extends R2dbcRepository<MeetingSite, Long> {
    Flux<MeetingSite> findAllByMeetingSiteIdInAndSiteId(List<String> meetingIds, Integer siteId);

    @Query("delete from clb_db.meeting_site m where m.start_date between :startTime and :endTime")
    Mono<Long> deleteAllByStartDateBetween(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

}