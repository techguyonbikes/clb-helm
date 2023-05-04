package com.tvf.clb.service.service;

import com.tvf.clb.base.dto.RaceResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;


@Service
@RequiredArgsConstructor
public class RaceRedisService {

    @Autowired
    private ReactiveRedisTemplate<Long, RaceResponseDto> raceDetailTemplate;

    public Mono<Boolean> saveRace(Long raceId, RaceResponseDto race){
        return this.raceDetailTemplate.opsForValue().set(raceId, race);
    }

    public Mono<RaceResponseDto> findByRaceId(Long key){
        return this.raceDetailTemplate.opsForValue().get(key);
    }

    public Mono<List<RaceResponseDto>> findAllByKeysRaceResponseDto(List<Long> keys){
        return this.raceDetailTemplate.opsForValue().multiGet(keys);
    }

    public Mono<Long> delete(List<Long> raceIds){
        return Flux.fromIterable(raceIds)
                .flatMap(raceDetailTemplate::delete).count();
    }

    public Mono<Long> delete(Long raceId){
        return raceDetailTemplate.delete(raceId);
    }

    public Mono<Boolean> hasKey(Long raceId) {
        return raceDetailTemplate.hasKey(raceId);
    }

    public Mono<Boolean> updateRaceAdvertisedStart(Long raceId, Instant advertisedStart) {
        if (advertisedStart != null) {
            return findByRaceId(raceId).flatMap(raceResponseDto -> {
                raceResponseDto.setAdvertisedStart(advertisedStart.toString());
                return saveRace(raceId, raceResponseDto);
            });
        }
        return Mono.empty();
    }
}
