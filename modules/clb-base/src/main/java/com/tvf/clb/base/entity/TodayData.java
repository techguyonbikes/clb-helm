package com.tvf.clb.base.entity;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.TreeMap;

@Component
public class TodayData {

    // Map race advertised start (timestamp) to race id
    private TreeMap<Long, Long> races;

    private Instant lastTimeCrawl = Instant.now();

    public void addRace(Long advertisedStart, Long raceId) {
        races.put(advertisedStart, raceId);
    }

    public void setRaces(TreeMap<Long, Long> races) {
        this.races = races;
    }

    public TreeMap<Long, Long> getRaces() {
        return races;
    }

    public Instant getLastTimeCrawl() {
        return lastTimeCrawl;
    }

    public void setLastTimeCrawl(Instant lastTimeCrawl) {
        this.lastTimeCrawl = lastTimeCrawl;
    }
}
