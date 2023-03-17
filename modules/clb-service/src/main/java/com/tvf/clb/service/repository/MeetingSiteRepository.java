package com.tvf.clb.service.repository;

import com.tvf.clb.base.entity.MeetingSite;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;

@Repository
public interface MeetingSiteRepository extends R2dbcRepository<MeetingSite, Long> {
    Flux<MeetingSite> findAllByMeetingSiteIdInAndSiteId(List<String> meetingIds, Integer siteId);

    Flux<MeetingSite> findAllByMeetingSiteIdIn(List<String> meetingSiteId);

}