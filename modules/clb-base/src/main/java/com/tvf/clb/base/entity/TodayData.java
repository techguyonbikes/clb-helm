package com.tvf.clb.base.entity;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TodayData {

    // Map race advertised start (epoch milliseconds) to race id
    private TreeMap<Long, List<Long>> races;

    private Instant lastTimeCrawl = Instant.now();

    public synchronized void addOrUpdateRace(Long advertisedStart, Long raceId) {

        List<Long> allRaceIds = races.values().stream().flatMap(Collection::stream).collect(Collectors.toList());

        if (! allRaceIds.contains(raceId)) {
            addRace(advertisedStart, raceId);
        } else {
            updateRaceAdvertisedStart(raceId, advertisedStart);
        }
    }

    public synchronized void addRace(Long advertisedStart, Long raceId) {
        List<Long> ids = races.get(advertisedStart);
        if (ids == null) {
            ids = new ArrayList<>();
        }
        ids.add(raceId);

        races.put(advertisedStart, ids);
    }

    public void setRaces(TreeMap<Long, List<Long>> races) {
        this.races = races;
    }

    public TreeMap<Long, List<Long>> getRaces() {
        return races;
    }

    public synchronized void deleteFinalOrAbandonedRace(Long advertisedStart, Long raceId) {
        if (races != null) {
            List<Long> raceIds = races.get(advertisedStart);
            if (raceIds != null) {
                raceIds.removeIf(id -> id.equals(raceId));
                if (raceIds.isEmpty()) {
                    races.remove(advertisedStart);
                }
            }
        }
    }

    public synchronized void updateRaceAdvertisedStart(Long raceId, Long newAdvertisedStart) {
        Long oldAdvertisedStart = null;
        for (Map.Entry<Long, List<Long>> entry : races.entrySet()) {
            Long startTime = entry.getKey();
            List<Long> raceIds = entry.getValue();
            if (raceIds.contains(raceId)) {
                oldAdvertisedStart = startTime;
                break;
            }
        }

        if (oldAdvertisedStart != null && !newAdvertisedStart.equals(oldAdvertisedStart)) {
            updateRaceAdvertisedStart(raceId, oldAdvertisedStart, newAdvertisedStart);
        }
    }

    public synchronized void updateRaceAdvertisedStart(Long raceId, Long oldAdvertisedStart, Long newAdvertisedStart) {
        deleteFinalOrAbandonedRace(oldAdvertisedStart, raceId);
        addRace(newAdvertisedStart, raceId);
    }

    public Instant getLastTimeCrawl() {
        return lastTimeCrawl;
    }

    public void setLastTimeCrawl(Instant lastTimeCrawl) {
        this.lastTimeCrawl = lastTimeCrawl;
    }
}
