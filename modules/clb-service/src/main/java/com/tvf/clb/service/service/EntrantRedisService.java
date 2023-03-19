package com.tvf.clb.service.service;

import com.tvf.clb.base.entity.EntrantResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;


@Service
@RequiredArgsConstructor
public class EntrantRedisService {

    @Autowired
    private ReactiveRedisTemplate<Long, List<EntrantResponseDto>> raceDetailTemplate;

    public Mono<Boolean> saveRace(Long raceId, List<EntrantResponseDto> entrants){
        return this.raceDetailTemplate.opsForValue().set(raceId, entrants);
    }

    public Mono<List<EntrantResponseDto>> getByKey(Long key){
        return this.raceDetailTemplate.opsForValue().get(key);
    }

    public Mono<Long> delete(List<Long> raceIds){
        return Flux.fromIterable(raceIds)
                .flatMap(raceDetailTemplate::delete).count();
    }

    public Mono<Long> delete(Long raceId){
        return raceDetailTemplate.delete(raceId);
    }
}
