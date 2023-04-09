package com.tvf.clb.base.entity;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TodayData {
    private Map<Long, Race> races;

    public void addRace(Long id, Race race) {
        races.put(id, race);
    }

    public void removeRaceById(Long id) {
        races.remove(id);
    }

    public Map<Long, Race> getRaces() {
        return races;
    }

    public void setRaces(Map<Long, Race> races) {
        this.races = races;
    }
}
