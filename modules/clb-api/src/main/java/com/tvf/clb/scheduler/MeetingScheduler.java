package com.tvf.clb.scheduler;

import com.tvf.clb.base.exception.ApiRequestFailedException;
import com.tvf.clb.service.service.ICrawlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
public class MeetingScheduler {

    @Qualifier("ladBrokeCrawlService")
    @Autowired
    private ICrawlService crawlService;

    /**
     * Crawl today meeting data at 00.00AM every day.
     */
    @Scheduled(cron = "0 0 0 ? * *")
    public void crawlTodayMeeting() {
        log.info("Start crawl today meeting");
        LocalDate dateObj = LocalDate.now();
        crawlService.getTodayMeetings(dateObj).onErrorComplete(ApiRequestFailedException.class::isInstance).subscribe();
    }

    /**
     * Crawl tomorrow meeting data at 00.15AM every day.
     */
    @Scheduled(cron = "0 15 0 ? * *")
    public void crawlTomorrowMeeting() {
        log.info("Start crawl tomorrow meeting");
        LocalDate dateObj = LocalDate.now().plusDays(1);
        crawlService.getTodayMeetings(dateObj).onErrorComplete(ApiRequestFailedException.class::isInstance).subscribe();
    }

    /**
     * Crawl the day after tomorrow meeting data at 00.30AM every day.
     */
    @Scheduled(cron = "0 30 0 ? * *")
    public void crawlTheDayAfterTomorrowMeeting() {
        log.info("Start crawl the day after tomorrow meeting");
        LocalDate dateObj = LocalDate.now().plusDays(2);
        crawlService.getTodayMeetings(dateObj).onErrorComplete(ApiRequestFailedException.class::isInstance).subscribe();
    }
}
