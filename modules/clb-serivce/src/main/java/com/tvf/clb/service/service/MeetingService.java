package com.tvf.clb.service.service;

import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.service.repository.MeetingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@Slf4j
public class MeetingService {

    @Autowired
    private MeetingRepository meetingRepository;

    public Flux<Meeting> getAllMeetings() {
        return meetingRepository.findAll();
    }

    public Mono<Meeting> getMeetingByMeetingId(UUID meetingId) {
        return meetingRepository.findByMeetingId(meetingId);
    }

}
