package com.tvf.clb.service.service;

import com.tvf.clb.base.dto.MeetingDto;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.service.repository.MeetingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.*;

@Service
@Slf4j
public class MeetingService {

    @Autowired
    private MeetingRepository meetingRepository;
    public Mono<Meeting> getMeetingByMeetingId(String meetingId) {
        return meetingRepository.findByMeetingId(meetingId);
    }

    public Flux<MeetingDto> filterMeetingByDate(){
        return meetingRepository.findMeetingByDate()
                .map(r -> {
                    r.setRaceType(String.valueOf(r.getRaceType().charAt(0)));
                    return r;
                });
    }

}
