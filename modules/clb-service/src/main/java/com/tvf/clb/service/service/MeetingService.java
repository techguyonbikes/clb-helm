package com.tvf.clb.service.service;

import com.tvf.clb.base.dto.MeetingOptions;
import com.tvf.clb.service.repository.MeetingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.*;

@Service
@Slf4j
public class MeetingService {

    @Autowired
    private MeetingRepository meetingRepository;

    public Flux<MeetingOptions> filterMeetingByDate(LocalDate date){
        LocalDateTime dateTime = date.atTime(LocalTime.MIN);
        Instant startDate = dateTime.atOffset(ZoneOffset.UTC).toInstant();
        return meetingRepository.findMeetingByDate(startDate);
    }
}
