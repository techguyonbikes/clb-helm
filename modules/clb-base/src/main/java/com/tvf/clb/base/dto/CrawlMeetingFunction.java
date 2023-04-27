package com.tvf.clb.base.dto;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@FunctionalInterface
public interface CrawlMeetingFunction {

    List<MeetingDto> crawl(LocalDate date) throws IOException;
}
