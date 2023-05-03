package com.tvf.clb.base.entity;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

@Component
public class TodayData {

    // Map race advertised start (timestamp) to race id
    private TreeMap<Long, List<Long>> races;

    private Instant lastTimeCrawl = Instant.now();

    public synchronized void addRace(Long advertisedStart, Long raceId) {
        List<Long> raceIds = races.get(advertisedStart);
        if (raceIds == null) {
            raceIds = new ArrayList<>();
        }
        if (! raceIds.contains(raceId)) {
            raceIds.add(raceId);
        }
        races.put(advertisedStart, raceIds);
    }

    public void setRaces(TreeMap<Long, List<Long>> races) {
        this.races = races;
    }

    public TreeMap<Long, List<Long>> getRaces() {
        return races;
    }

    public void deleteFinalOrAbandonedRace(Long advertisedStart, Long raceId) {
        List<Long> raceIds = races.get(advertisedStart);
        raceIds.removeIf(id -> id.equals(raceId));
        if (raceIds.isEmpty()) {
            races.remove(advertisedStart);
        }
    }


    public Instant getLastTimeCrawl() {
        return lastTimeCrawl;
    }

    public void setLastTimeCrawl(Instant lastTimeCrawl) {
        this.lastTimeCrawl = lastTimeCrawl;
    }
}
