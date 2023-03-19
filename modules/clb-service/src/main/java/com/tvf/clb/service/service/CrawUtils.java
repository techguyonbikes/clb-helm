package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tvf.clb.base.dto.MeetingMapper;
import com.tvf.clb.base.dto.RaceResponseMapper;
import com.tvf.clb.base.entity.*;
import com.tvf.clb.service.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CrawUtils {

    @Autowired
    private MeetingSiteRepository meetingSiteRepository;

    @Autowired
    private RaceSiteRepository raceSiteRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private RaceRepository raceRepository;

    @Autowired
    private EntrantRedisService entrantRedisService;

    private Gson gson = new Gson();

    @Autowired
    private ReactiveRedisTemplate<String, Long> raceNameAndIdTemplate;

    public void saveEntrantIntoRedis(List<Entrant> entrants, Integer site, String raceName, Integer raceNumber) {
        Mono<Long> raceIdMono = raceNameAndIdTemplate.opsForValue().get(raceName);
        raceIdMono.map(raceId -> {
            Mono<List<EntrantResponseDto>> entrantStored = entrantRedisService.findEntrantByRaceId(raceId);
            return entrantStored.subscribe(records -> {
                Type listType = new TypeToken<List<EntrantResponseDto>>(){}.getType();
                List<EntrantResponseDto> storeRecords = gson.fromJson(gson.toJson(records), listType);
                Map<String, Entrant> entrantMap = new HashMap<>();
                for(Entrant entrant: entrants) {
                    entrantMap.put(entrant.getName(), entrant);
                }
                for (EntrantResponseDto entrantResponseDto: storeRecords) {
                    Entrant newEntrant = entrantMap.get(entrantResponseDto.getName());
                    if(entrantResponseDto.getPriceFluctuations() == null) {
                        Map<Integer, List<Double>> price = new HashMap<>();
                        log.error("null price");
                        entrantResponseDto.setPriceFluctuations(price);
                    }
                    Map<Integer, List<Double>> price = entrantResponseDto.getPriceFluctuations();
                    price.put(site, newEntrant.getPrices());
                }
                entrantRedisService.saveRace(raceId, storeRecords).subscribe();
            });
        }).subscribe();
    }

    public void saveMeetingSite(List<Meeting> meetings, Integer site) {
        Flux<MeetingSite> newMeetingSite = Flux.fromIterable(meetings).flatMap(
                r -> {
                    Mono<Long> generalId = meetingRepository.getMeetingId(r.getName(), r.getRaceType(), r.getAdvertisedDate());
                    return Flux.from(generalId).map(id -> MeetingMapper.toMetingSite(r, site, id));
                }
        );
        Flux<MeetingSite> existedMeetingSite = meetingSiteRepository
                .findAllByMeetingSiteIdInAndSiteId(meetings.stream().map(Meeting::getMeetingId).collect(Collectors.toList()), site);

        Flux.zip(newMeetingSite.collectList(), existedMeetingSite.collectList())
                .doOnNext(tuple2 -> {
                    tuple2.getT2().forEach(dup -> tuple2.getT1().remove(dup));
                    log.info("Meeting site " + site + " need to be update is " + tuple2.getT1().size());
                    meetingSiteRepository.saveAll(tuple2.getT1()).subscribe();
                }).subscribe();
    }

    public void saveRaceSite(List<Race> races, Integer site) {
        Flux<RaceSite> newMeetingSite = Flux.fromIterable(races.stream().filter(x -> x.getNumber() != null).collect(Collectors.toList())).flatMap(
                race -> {
                    Mono<Long> generalId = raceRepository.getRaceId(race.getName(), race.getNumber(), race.getAdvertisedStart());
                    return Flux.from(generalId).map(id -> RaceResponseMapper.toRacesiteDto(race, site, id));
                }
        );
        Flux<RaceSite> existedMeetingSite = raceSiteRepository
                .findAllByRaceSiteIdInAndSiteId(races.stream().map(Race::getRaceId).collect(Collectors.toList()), site);

        Flux.zip(newMeetingSite.collectList(), existedMeetingSite.collectList())
                .doOnNext(tuple2 -> {
                    tuple2.getT2().forEach(dup -> tuple2.getT1().remove(dup));
                    log.info("Race site " + site + " need to be update is " + tuple2.getT1().size());
                    raceSiteRepository.saveAll(tuple2.getT1()).subscribe();
                }).subscribe();

    }
}
