package com.tvf.clb.service.service;

import com.tvf.clb.base.dto.MeetingDto;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ICrawlService {

    Flux<MeetingDto> getTodayMeetings(LocalDate date);

    public Map<String, Map<Integer, List<Double>>> getEntrantByRaceId(String raceId);
}
