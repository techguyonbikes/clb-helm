package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tvf.clb.base.dto.EntrantMapper;
import com.tvf.clb.base.dto.MeetingMapper;
import com.tvf.clb.base.dto.RaceResponseMapper;
import com.tvf.clb.base.dto.SiteEnum;
import com.tvf.clb.base.entity.*;
import com.tvf.clb.base.model.CrawlEntrantData;
import com.tvf.clb.service.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CrawUtils {

    @Autowired
    private ServiceLookup serviceLookup;

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

    @Autowired
    private ReactiveRedisTemplate<String, Long> raceNameAndIdTemplate;

    public void saveEntrantIntoRedis(List<Entrant> entrants, Integer site, String raceName, String raceUUID) {
        Mono<Long> raceIdMono = raceNameAndIdTemplate.opsForValue().get(raceName);
        raceIdMono.map(raceId -> {
            Mono<List<EntrantResponseDto>> entrantStored = entrantRedisService.findEntrantByRaceId(raceId);
            return entrantStored.subscribe(records -> {

                List<EntrantResponseDto> storeRecords = EntrantMapper.convertFromRedisPriceToDTO(records);
                Map<String, Entrant> entrantMap = new HashMap<>();
                for (Entrant entrant : entrants) {
                    entrantMap.put(entrant.getName(), entrant);
                }
                for (EntrantResponseDto entrantResponseDto : storeRecords) {
                    Entrant newEntrant = entrantMap.get(entrantResponseDto.getName());
                    if (newEntrant != null) {
                        if (entrantResponseDto.getPriceFluctuations() == null) {
                            Map<Integer, List<Float>> price = new HashMap<>();
                            log.error("null price");
                            entrantResponseDto.setPriceFluctuations(price);
                        }
                        // todo: the position in neds and labBroke is not correct

                        Map<Integer, List<Float>> price = entrantResponseDto.getPriceFluctuations();
                        price.put(site, newEntrant.getPrices() == null ? new ArrayList<>() : newEntrant.getPrices());

                        Map<Integer, String> mapRaceUUID = entrantResponseDto.getRaceUUID();
                        mapRaceUUID.put(site, raceUUID);
                    }
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

    public Mono<Map<Long, CrawlEntrantData>> crawlNewPriceByRaceUUID(Map<Integer, String> mapSiteRaceUUID, Map<String, Long> entrantIdMapName) {
        Map<Long, CrawlEntrantData> newPrices = new HashMap<>();
        return Flux.fromIterable(mapSiteRaceUUID.entrySet())
                .parallel().runOn(Schedulers.parallel())
                .map(entry ->
                        serviceLookup.forBean(ICrawlService.class, SiteEnum.getSiteNameById(entry.getKey()))
                                .getEntrantByRaceUUID(entry.getValue(), entrantIdMapName))
                .sequential()
                .doOnNext(prices -> prices.forEach((key, value) -> {
                    if (newPrices.containsKey(key)) {
                        newPrices.get(key).getPriceMap().putAll(value.getPriceMap());
                    } else {
                        newPrices.put(key, value);
                    }
                })).then(Mono.just(newPrices));
    }

}
