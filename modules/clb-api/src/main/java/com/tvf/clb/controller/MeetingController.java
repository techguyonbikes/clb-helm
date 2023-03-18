package com.tvf.clb.controller;

import com.tvf.clb.base.dto.MeetingDto;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.service.service.CrawlService;
import com.tvf.clb.service.service.ICrawlService;
import com.tvf.clb.service.service.MeetingService;
import com.tvf.clb.service.service.ServiceLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/meeting")
public class MeetingController {

    @Autowired
    private List<ICrawlService> crawlServiceList;

    @Autowired
    private ServiceLookup serviceLookup;
    @Autowired
    private MeetingService meetingService;

    @GetMapping("/crawl")
    public Flux<MeetingDto> crawlTodayMeeting(@RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        ICrawlService crawlService = serviceLookup.forBean(ICrawlService.class, AppConstant.LAD_BROKE);
        Flux<MeetingDto> result = crawlService.getTodayMeetings(date);
        result.doOnComplete(() -> {

        });
        return result;
    }

    @GetMapping("")
    public Flux<MeetingDto> getListMeeting(@RequestParam(value = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return meetingService.filterMeetingByDate(date);
    }

    private Flux<MeetingDto> getMeetingFromAllSite(LocalDate date) {
        List<ICrawlService> crawlServices = new ArrayList<>();
        for (String site: AppConstant.SITE_LIST) {
            crawlServices.add(serviceLookup.forBean(ICrawlService.class, site));
        }
        return Flux.fromIterable(crawlServices)
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(x -> x.getTodayMeetings(date))
                .sequential();
    }
}
