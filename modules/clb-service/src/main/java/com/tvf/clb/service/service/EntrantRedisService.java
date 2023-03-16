package com.tvf.clb.service.service;

import com.tvf.clb.base.entity.EntrantRedis;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;


@Service
@RequiredArgsConstructor
public class EntrantRedisService {

    private final ReactiveRedisOperations<Long, List<EntrantRedis>> entrantReactiveRedisOperations;

    public Mono<Boolean> saveAll(List<EntrantRedis> entrants){
        return this.entrantReactiveRedisOperations.opsForValue().set(entrants.get(0).getRaceId(), entrants);
    }

    public Mono<List<EntrantRedis>> getByKey(Long key){
        return this.entrantReactiveRedisOperations.opsForValue().get(key);
    }

    public Mono<Long> delete(List<Long> raceIds){
        return Flux.fromIterable(raceIds)
                .flatMap(entrantReactiveRedisOperations::delete).count();
    }

    public Mono<Long> delete(Long raceId){
        return entrantReactiveRedisOperations.delete(raceId);
    }
}
