package com.tvf.clb.base.dto;

import java.io.IOException;

@FunctionalInterface
public interface CrawlRaceFunction {
    Object crawl(String raceUUID) throws IOException;
}
