package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.entity.*;
import com.tvf.clb.base.model.CrawlEntrantData;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.CommonUtils;
import com.tvf.clb.service.repository.*;
import io.r2dbc.postgresql.codec.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
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
    private EntrantRepository entrantRepository;

    @Autowired
    private ReactiveRedisTemplate<String, Long> raceNameAndIdTemplate;

    public void saveEntrantIntoRedis(List<Entrant> entrants, Integer site, String raceName, String raceUUID,
                                     String statusRace, Instant advertisedStart, Integer raceNumber, String meetingType) {
        Mono<Long> raceIdMono = raceNameAndIdTemplate.opsForValue().get(raceName).switchIfEmpty(
                raceRepository.getRaceIdByMeetingType(meetingType, raceNumber, advertisedStart)
        );
        raceIdMono.subscribe(raceId -> {
            Mono<List<EntrantResponseDto>> entrantStored = entrantRedisService.findEntrantByRaceId(raceId);
            entrantStored.subscribe(records -> {

                List<EntrantResponseDto> storeRecords = EntrantMapper.convertFromRedisPriceToDTO(records);
                Map<Integer, Entrant> entrantMap = new HashMap<>();
                for (Entrant entrant : entrants) {
                    entrantMap.put(entrant.getNumber(), entrant);
                }
                for (EntrantResponseDto entrantResponseDto : storeRecords) {
                    Entrant newEntrant = entrantMap.get(entrantResponseDto.getNumber());

                    if (entrantResponseDto.getPriceFluctuations() == null) {
                        Map<Integer, List<Float>> price = new HashMap<>();
                        log.error("null price");
                        entrantResponseDto.setPriceFluctuations(price);
                    }
                    if (site.equals(AppConstant.POINT_BET_SITE_ID)){
                        entrantResponseDto.setStatusRace(statusRace);
                    }

                    // todo: the position in neds and labBroke is not correct
                    if (newEntrant != null) {
                        Map<Integer, List<Float>> price = entrantResponseDto.getPriceFluctuations();
                        price.put(site, newEntrant.getPrices() == null ? new ArrayList<>() : newEntrant.getCurrentSitePrice());
                    }
                    Map<Integer, String> mapRaceUUID = entrantResponseDto.getRaceUUID();
                    mapRaceUUID.put(site, raceUUID);
                }

                entrantRedisService.saveRace(raceId, storeRecords).subscribe();
            });
        });
    }

    public void saveMeetingSite(List<Meeting> meetings, Integer site) {
        Flux<MeetingSite> newMeetingSite = Flux.fromIterable(meetings).flatMap(
                r -> {
                    Mono<Long> generalId = meetingRepository.getMeetingId(r.getName(), r.getRaceType(), r.getAdvertisedDate())
                            .switchIfEmpty(
                                        meetingRepository.getMeetingIdDiffName(CommonUtils.checkDiffStateMeeting(r.getState()), r.getRaceType(), r.getAdvertisedDate())
                            );
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
                    Mono<Long> generalId = raceRepository.getRaceIdByMeetingType(race.getRaceType(), race.getNumber(), race.getAdvertisedStart());
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

    public void saveRaceSiteAndUpdateStatue(List<RaceDto> raceDtoList, Integer site) {
        if (!raceDtoList.isEmpty()) {
            Flux<RaceSite> newMeetingSite = Flux.fromIterable(raceDtoList.stream().filter(x -> x.getNumber() != null).collect(Collectors.toList())).flatMap(
                    race -> {
                        Mono<Long> generalId = raceRepository.getRaceIdByMeetingType(race.getRaceType(), race.getNumber(), race.getAdvertisedStart())
                                .switchIfEmpty(Mono.empty());
                        return Flux.from(generalId).map(id -> {
                                    raceRepository.setUpdateRaceStatusById(id, race.getStatus()).subscribe();
                                    return RaceResponseMapper.toRaceSiteDto(race, site, id);
                                }
                        );
                    }
            );
            Flux<RaceSite> existedMeetingSite = raceSiteRepository
                    .findAllByRaceSiteIdInAndSiteId(raceDtoList.stream().map(RaceDto::getId).collect(Collectors.toList()), site).switchIfEmpty(Flux.empty());

            Flux.zip(newMeetingSite.collectList(), existedMeetingSite.collectList())
                    .doOnNext(tuple2 -> {
                        tuple2.getT2().forEach(dup -> tuple2.getT1().remove(dup));
                        log.info("Race site " + site + " need to be update is " + tuple2.getT1().size());
                        raceSiteRepository.saveAll(tuple2.getT1()).subscribe();
                    }).subscribe();
        }
    }

    public Mono<Map<Integer, CrawlEntrantData>> crawlNewPriceByRaceUUID(Map<Integer, String> mapSiteRaceUUID) {
        Map<Integer, CrawlEntrantData> newPrices = new HashMap<>();
        return Flux.fromIterable(mapSiteRaceUUID.entrySet())
                .parallel().runOn(Schedulers.parallel())
                .map(entry ->
                        serviceLookup.forBean(ICrawlService.class, SiteEnum.getSiteNameById(entry.getKey()))
                                .getEntrantByRaceUUID(entry.getValue()))
                .sequential()
                .doOnNext(prices -> prices.forEach((key, value) -> {
                    if (newPrices.containsKey(key)) {
                        newPrices.get(key).getPriceMap().putAll(value.getPriceMap());
                        if (Objects.equals(value.getSiteId(), AppConstant.LAD_BROKE_SITE_ID))
                            newPrices.get(key).setPosition(value.getPosition());
                        if (Objects.equals(value.getSiteId(), AppConstant.POINT_BET_SITE_ID))
                            newPrices.get(key).setStatusRace(value.getStatusRace());
                    } else {
                        newPrices.put(key, value);
                    }
                })).then(Mono.just(newPrices));
    }
    public void saveRaceSitebyTab(List<Race> races, Integer site) {
        Flux<RaceSite> newMeetingSite = Flux.fromIterable(races.stream().filter(x -> x.getNumber() != null).collect(Collectors.toList())).flatMap(
                race -> {
                    if (race.getDistance() == null){
                        System.out.println();
                    }
                    Mono<Long> generalId = raceRepository.getRaceIdbyDistance(race.getDistance(), race.getNumber(), race.getAdvertisedStart());
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

    public void saveEntrantsPriceIntoDB(List<Entrant> newEntrant, RaceDto raceDto, Integer siteId) {

        Gson gson = new Gson();
        Flux<Entrant> existedFlux = entrantRepository.findAllEntrantsInRace(raceDto.getMeetingName(), raceDto.getRaceType(), raceDto.getNumber(), raceDto.getAdvertisedStart());
        List<Entrant> listNeedToUpdate = new ArrayList<>();
        existedFlux.collectList().subscribe(listExisted -> {

                Map<Integer, Entrant> mapNumberToNewEntrant = newEntrant.stream().collect(Collectors.toMap(Entrant::getNumber, Function.identity()));
                listExisted.forEach(existed -> {

                        Map<Integer, List<Float>> allExistedSitePrices = CommonUtils.getSitePriceFromJsonb(existed.getPriceFluctuations());

                        List<Float> existedSitePrice = allExistedSitePrices.get(siteId);
                        List<Float> newSitePrice = mapNumberToNewEntrant.get(existed.getNumber()).getCurrentSitePrice();

                        if (! Objects.equals(existedSitePrice, newSitePrice)) {
                            allExistedSitePrices.put(siteId, newSitePrice);
                            existed.setPriceFluctuations(Json.of(gson.toJson(allExistedSitePrices)));

                            listNeedToUpdate.add(existed);
                        }
                    }
                );
                entrantRepository.saveAll(listNeedToUpdate).subscribe();
            }
        );
    }

}
