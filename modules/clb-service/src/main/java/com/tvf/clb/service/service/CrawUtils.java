package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.entity.*;
import com.tvf.clb.base.model.CrawlRaceData;
import com.tvf.clb.base.model.EntrantRawData;
import com.tvf.clb.base.model.LadbrokesMarketsRawData;
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
    private RaceRedisService raceRedisService;

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
            Mono<RaceResponseDto> raceStoredMono = raceRedisService.findByRaceId(raceId);

            raceStoredMono.subscribe(raceStored -> {

                Map<Integer, Entrant> newEntrantMap = new HashMap<>();
                for (Entrant entrant : entrants) {
                    newEntrantMap.put(entrant.getNumber(), entrant);
                }
                for (EntrantResponseDto entrantResponseDto : raceStored.getEntrants()) {
                    Entrant newEntrant = newEntrantMap.get(entrantResponseDto.getNumber());

                    if (entrantResponseDto.getPriceFluctuations() == null) {
                        Map<Integer, List<Float>> price = new HashMap<>();
                        log.error("Entrant id = {} in race id = {} null price", entrantResponseDto.getId(), raceStored.getId());
                        entrantResponseDto.setPriceFluctuations(price);
                    }

                    if (newEntrant != null) {
                        Map<Integer, List<Float>> price = entrantResponseDto.getPriceFluctuations();
                        price.put(site, newEntrant.getCurrentSitePrice() == null ? new ArrayList<>() : newEntrant.getCurrentSitePrice());
                    }
                }

                if (raceStored.getStatus() == null && statusRace != null) {
                    raceStored.setStatus(statusRace);
                }

                Map<Integer, String> mapRaceUUID = raceStored.getMapSiteUUID();
                mapRaceUUID.put(site, raceUUID);

                raceRedisService.saveRace(raceId, raceStored).subscribe();
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

    public void saveRaceSiteAndUpdateStatus(List<RaceDto> raceDtoList, Integer site) {
        if (!raceDtoList.isEmpty()) {
            Flux<RaceSite> newMeetingSite = Flux.fromIterable(raceDtoList.stream().filter(x -> x.getNumber() != null).collect(Collectors.toList())).flatMap(
                    race -> {
                        Mono<Long> generalId = raceRepository.getRaceIdByMeetingType(race.getRaceType(), race.getNumber(), race.getAdvertisedStart())
                                .switchIfEmpty(Mono.empty());
                        return Flux.from(generalId).map(id -> {
                                    if (site.equals(AppConstant.ZBET_SITE_ID)) {
                                        raceRepository.setUpdateRaceStatusById(id, race.getStatus()).subscribe();
                                    }
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

    public Mono<CrawlRaceData> crawlNewDataByRaceUUID(Map<Integer, String> mapSiteRaceUUID) {
        CrawlRaceData result = new CrawlRaceData();
        result.setMapEntrants(new HashMap<>());
        return Flux.fromIterable(mapSiteRaceUUID.entrySet())
                .parallel().runOn(Schedulers.parallel())
                .map(entry ->
                        serviceLookup.forBean(ICrawlService.class, SiteEnum.getSiteNameById(entry.getKey()))
                                .getEntrantByRaceUUID(entry.getValue()))
                .sequential()
                .doOnNext(raceNewData -> {

                    if (result.getStatus() == null && raceNewData.getStatus() != null) {
                        result.setStatus(raceNewData.getStatus());
                    }

                    raceNewData.getMapEntrants().forEach((key, value) -> {
                        if (result.getMapEntrants().containsKey(key)) {
                            result.getMapEntrants().get(key).getPriceMap().putAll(value.getPriceMap());
                            if (raceNewData.getSiteId().equals(SiteEnum.ZBET.getId())) {
                                result.getMapEntrants().get(key).setPosition(value.getPosition());
                            }
                        } else {
                            result.getMapEntrants().put(key, value);
                        }
                    });
                })
                .then(Mono.just(result));
    }
    public void saveRaceSitebyTab(List<Race> races, Integer site) {
        Flux<RaceSite> newMeetingSite = Flux.fromIterable(races.stream().filter(x -> x.getNumber() != null).collect(Collectors.toList())).flatMap(
                race -> {
                    Mono<Long> generalId = raceRepository.getRaceIdbyDistance(race.getDistance(), race.getNumber(), race.getAdvertisedStart())
                            .switchIfEmpty(raceRepository.getRaceIdByMeetingType(race.getRaceType(), race.getNumber(), race.getAdvertisedStart()));
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

    public List<EntrantRawData> getListEntrant(LadBrokedItRaceDto raceDto, Map<String, ArrayList<Float>> allEntrantPrices, String raceId, Map<String, Integer> positions) {
        LadbrokesMarketsRawData marketsRawData = raceDto.getMarkets().values().stream()
                .filter(m -> m.getName().equals(AppConstant.MARKETS_NAME)).findFirst()
                .orElseThrow(() -> new RuntimeException("No markets found"));;

        List<EntrantRawData> result = new ArrayList<>();

        marketsRawData.getRace_id().forEach(x -> {
            EntrantRawData data = raceDto.getEntrants().get(x);
            if (data.getFormSummary() != null && data.getId() != null) {
                EntrantRawData entrantRawData = EntrantMapper.mapPrices(
                        data,
                        allEntrantPrices == null ? new ArrayList<>() : allEntrantPrices.getOrDefault(data.getId(), new ArrayList<>()),
                        positions.getOrDefault(data.getId(), 0)
                );
                entrantRawData.setRaceId(raceId);
                result.add(entrantRawData);
            }
        });

        return result;
    }

}
