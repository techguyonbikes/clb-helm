package com.tvf.clb.api.controller;

import com.tvf.clb.base.dto.MeetingDto;
import com.tvf.clb.service.service.CrawlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/meeting")
public class MeetingController {

    @Autowired
    private CrawlService crawlService;

    @GetMapping("/crawl")
    public Mono<List<MeetingDto>> crawlTodayMeeting(@RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return crawlService.getTodayMeetings(date);
    }

    @GetMapping("")
    public Mono<List<MeetingDto>> getTodayMeeting(@RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return crawlService.getTodayMeetings(date);
    }
}
