package com.tvf.clb.service.service;

import com.tvf.clb.base.dto.MeetingDto;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

public interface ICrawlService {

    Flux<MeetingDto> getTodayMeetings(LocalDate date);
}
