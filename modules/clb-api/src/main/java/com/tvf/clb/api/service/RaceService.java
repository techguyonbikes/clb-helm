package com.tvf.clb.api.service;

import com.tvf.clb.api.repository.RaceRepository;
import com.tvf.clb.base.entity.Race;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class RaceService {
    @Autowired
    private RaceRepository raceRepository;

    public Flux<Race> getAllRace() {
        return raceRepository.findAll();
    }
}
