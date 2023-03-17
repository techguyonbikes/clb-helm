package com.tvf.clb.controller;

import com.tvf.clb.base.dto.MeetingDto;
import com.tvf.clb.service.service.CrawlService;
import com.tvf.clb.service.service.ICrawlService;
import com.tvf.clb.service.service.MeetingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@RestController
@RequestMapping("/meeting")
public class MeetingController {

    @Autowired
    private ICrawlService crawlService1;

    @Autowired
    private CrawlService crawlService;

    @Autowired
    private MeetingService meetingService;

    @GetMapping("/crawl")
    public Flux<MeetingDto> crawlTodayMeeting(@RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return crawlService1.getTodayMeetings(date);
    }

    @GetMapping("")
    public Flux<MeetingDto> getListMeeting(@RequestParam(value = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return meetingService.filterMeetingByDate(date);
    }
}
