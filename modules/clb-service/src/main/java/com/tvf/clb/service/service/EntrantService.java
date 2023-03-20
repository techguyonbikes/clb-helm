package com.tvf.clb.service.service;

import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.EntrantResponseDto;
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
    EntrantRedisService entrantRedisService;

    public Flux<Entrant> getAllEntrant() {
        return entrantRepository.findAll();

    }
    public Flux<EntrantResponseDto> getEntrantsByRaceId(Long raceId){
        return entrantRedisService.findEntrantByRaceId(raceId).flatMapMany(Flux::fromIterable);
    }


}
