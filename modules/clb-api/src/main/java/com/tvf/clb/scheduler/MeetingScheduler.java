package com.tvf.clb.scheduler;

import com.tvf.clb.service.service.CrawlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
public class MeetingScheduler {

    @Autowired
    private CrawlService crawlService;

    /**
     * Crawling meeting data current date and two next days at 00.00AM every day.
     */
    @Scheduled(cron = "0 0 0 ? * *")
    public void dailyMeetingsCrawling() {
        LocalDate dateObj = LocalDate.now();
        log.info("Start Crawl Data from v2/racing/meeting?date=" + dateObj + "&region=domestic&timezone=Asia%2FBangkok");
        crawlService.getTodayMeetings(dateObj).subscribe();

        dateObj = dateObj.plusDays(1);
        log.info("Start Crawl Data from v2/racing/meeting?date=" + dateObj + "&region=domestic&timezone=Asia%2FBangkok");
        crawlService.getTodayMeetings(dateObj).subscribe();

        dateObj = dateObj.plusDays(1);
        log.info("Start Crawl Data from v2/racing/meeting?date=" + dateObj + "&region=domestic&timezone=Asia%2FBangkok");
        crawlService.getTodayMeetings(dateObj).subscribe();

        log.info("---------------------------------------------------");
    }

}
