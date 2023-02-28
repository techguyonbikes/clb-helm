package com.tvf.clb.api.controller;

import com.tvf.clb.api.service.CrawlService;
import com.tvf.clb.base.dto.MeetingDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.scheduling.annotation.Async;
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

    @GetMapping("")
    public Mono<List<MeetingDto>> getTodayMeeting(@RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return crawlService.getTodayMeetings(date);
    }
}
