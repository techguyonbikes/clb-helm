package com.tvf.clb.controller;

import com.tvf.clb.base.dto.MeetingDto;
import com.tvf.clb.base.dto.MeetingFilterDTO;
import com.tvf.clb.service.service.CrawlService;
import com.tvf.clb.service.service.MeetingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/meeting")
public class MeetingController {

    @Autowired
    private CrawlService crawlService;

    @Autowired
    private MeetingService meetingService;

    @GetMapping("/crawl")
    public Mono<List<MeetingDto>> crawlTodayMeeting(@RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return crawlService.getTodayMeetings(date);
    }
    @GetMapping("")
    public Flux<MeetingFilterDTO> getTodayMeeting(@RequestParam(value = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return meetingService.filterMeetingByDate(date);
    }
}
