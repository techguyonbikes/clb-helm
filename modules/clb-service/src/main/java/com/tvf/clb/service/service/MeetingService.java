package com.tvf.clb.service.service;

import com.tvf.clb.base.dto.MeetingDto;
import com.tvf.clb.base.dto.MeetingOptions;
import com.tvf.clb.base.dto.RaceDto;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.model.MeetingRawData;
import com.tvf.clb.service.repository.MeetingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.*;
import java.util.List;

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
