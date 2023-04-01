package com.tvf.clb.service.service;

import com.tvf.clb.base.dto.MeetingDto;
import com.tvf.clb.base.model.CrawlEntrantData;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.Map;

public interface ICrawlService {

    Flux<MeetingDto> getTodayMeetings(LocalDate date);

    public Map<Integer, CrawlEntrantData> getEntrantByRaceUUID(String raceId);
}
