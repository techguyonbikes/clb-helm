package com.tvf.clb.service.service;

import com.tvf.clb.base.entity.Race;
import com.tvf.clb.service.repository.RaceRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Component
@DependsOn("flyway")
@Getter
@Setter
@RequiredArgsConstructor
public class TodayData {

    // Map race advertised start (epoch milliseconds) to race id
    private SortedMap<Long, List<Long>> races;

    private Instant lastTimeCrawl = Instant.now();

    private final RaceRepository raceRepository;

    @PostConstruct
    public void postConstruct() {
        races = new TreeMap<>();
        Instant startTime = Instant.now().atZone(ZoneOffset.UTC).with(LocalTime.MIN).minusHours(5).toInstant();
        Instant endOfToday = Instant.now().atZone(ZoneOffset.UTC).with(LocalTime.MAX).toInstant();

        raceRepository.findAllByAdvertisedStartBetweenAndStatusNotIn(startTime, endOfToday, Collections.emptyList())
                      .doOnNext(race -> addOrUpdateRace(race.getAdvertisedStart().toEpochMilli(), race.getId()))
                      .blockLast();
    }

    public synchronized void addOrUpdateRaces(List<Race> newRaces) {
        List<Long> allRaceIds = races.values().stream().flatMap(Collection::stream).collect(Collectors.toList());

        newRaces.forEach(newRace -> {
            Long newRaceId = newRace.getId();
            Long newRaceAdvertisedStart = newRace.getAdvertisedStart().toEpochMilli();
            if (! allRaceIds.contains(newRaceId)) {
                addRace(newRaceAdvertisedStart, newRaceId);
            } else {
                updateRaceAdvertisedStart(newRaceId, newRaceAdvertisedStart);
            }
        });
    }

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

    public synchronized void deleteFinalOrAbandonedRace(Long advertisedStart, Long raceId) {
        List<Long> raceIds = races.get(advertisedStart);
        if (raceIds != null) {
            raceIds.removeIf(id -> id.equals(raceId));
            if (raceIds.isEmpty()) {
                races.remove(advertisedStart);
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
}
