package com.tvf.clb.service.service;

import com.tvf.clb.base.dto.MeetingFilterDTO;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.service.repository.MeetingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@Slf4j
public class MeetingService {

    @Autowired
    private MeetingRepository meetingRepository;
    public Mono<Meeting> getMeetingByMeetingId(String meetingId) {
        return meetingRepository.findByMeetingId(meetingId);
    }

    public Flux<MeetingFilterDTO> filterMeetingByDate(LocalDate date){
        LocalDateTime maxDateTime = date.atTime(23, 59, 59);
        LocalDateTime minDateTime = date.atTime(0, 0, 0);
        Instant endTime = maxDateTime.atOffset(ZoneOffset.UTC).toInstant();
        Instant startTime = minDateTime.atOffset(ZoneOffset.UTC).toInstant();
        return meetingRepository.findMeetingByDate(endTime, startTime)
                .map(r -> {
                    r.setType(String.valueOf(r.getType().charAt(0)));
                    return r;
                });
    }

}
