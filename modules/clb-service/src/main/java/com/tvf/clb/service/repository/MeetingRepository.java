package com.tvf.clb.service.repository;

import com.tvf.clb.base.entity.Meeting;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface MeetingRepository extends R2dbcRepository<Meeting, Long> {
    Mono<Meeting> findByMeetingId(String meetingId);
    Flux<Meeting> findAllByMeetingIdIn(List<String> meetingIds);
}
