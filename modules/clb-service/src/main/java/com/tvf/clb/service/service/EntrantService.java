package com.tvf.clb.service.service;

import com.tvf.clb.base.dto.EntrantMapper;
import com.tvf.clb.base.dto.EntrantResponseDto;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.service.repository.EntrantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class EntrantService {
    @Autowired
    EntrantRepository entrantRepository;

    @Autowired
    RaceRedisService raceRedisService;


    public Flux<Entrant> getAllEntrant() {
        return entrantRepository.findAll();

    }
    public Flux<EntrantResponseDto> getEntrantsByRaceId(Long raceId){
        return raceRedisService.findByRaceId(raceId)
                .flatMapMany(race -> Flux.fromIterable(race.getEntrants()))
                .switchIfEmpty(getEntrantsInDBByRaceId(raceId));
    }

    public Flux<EntrantResponseDto> getEntrantsInDBByRaceId(Long raceId) {
        return entrantRepository.getAllByRaceId(raceId).map(EntrantMapper::toEntrantResponseDto);
    }


}
