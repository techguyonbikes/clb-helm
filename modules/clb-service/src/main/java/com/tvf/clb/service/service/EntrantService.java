package com.tvf.clb.service.service;

import com.tvf.clb.base.dto.EntrantMapper;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.EntrantResponseDto;
import com.tvf.clb.base.entity.RaceSite;
import com.tvf.clb.service.repository.EntrantRepository;
import com.tvf.clb.service.repository.RaceSiteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EntrantService {
    @Autowired
    EntrantRepository entrantRepository;

    @Autowired
    EntrantRedisService entrantRedisService;

    @Autowired
    RaceSiteRepository raceSiteRepository;


    public Flux<Entrant> getAllEntrant() {
        return entrantRepository.findAll();

    }
    public Flux<EntrantResponseDto> getEntrantsByRaceId(Long raceId){
        return entrantRedisService.findEntrantByRaceId(raceId).flatMapMany(Flux::fromIterable)
                .switchIfEmpty(getEntrantsInDBByRaceId(raceId));
    }

    public Flux<EntrantResponseDto> getEntrantsInDBByRaceId(Long raceId) {
        Flux<RaceSite> raceSitesFlux = raceSiteRepository.getAllByGeneralRaceId(raceId);
        return raceSitesFlux.collectList().flatMapMany(raceSites -> {
                    Map<Integer, String> mapRaceSiteToUUID = raceSites.stream().collect(Collectors.toMap(RaceSite::getSiteId, RaceSite::getRaceSiteId));
                    return entrantRepository.getAllByRaceId(raceId).map(entrant -> EntrantMapper.toEntrantResponseDto(entrant, mapRaceSiteToUUID));
                }
        );
    }


}
