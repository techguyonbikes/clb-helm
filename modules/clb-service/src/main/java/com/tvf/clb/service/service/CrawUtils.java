package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.entity.*;
import com.tvf.clb.base.model.CrawlEntrantData;
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

import java.lang.reflect.Type;
import java.time.temporal.ChronoUnit;
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

    public void saveEntrantCrawlDataToRedis(List<Entrant> entrants, Integer site, String raceRedisKey, RaceDto raceDto) {

        raceNameAndIdTemplate.opsForValue().get(raceRedisKey).switchIfEmpty(getRaceByTypeAndNumberAndRangeAdvertisedStart(raceDto))
            .subscribe(raceId -> {
            Mono<RaceResponseDto> raceStoredMono = raceRedisService.findByRaceId(raceId);

            raceStoredMono.subscribe(raceStored -> saveEntrantDataToRedis(entrants, site, raceStored, raceDto, raceId));
        });
    }

    public void saveEntrantDataToRedis(List<Entrant> entrants, Integer site, RaceResponseDto raceStored, RaceDto raceDto, Long raceId){
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

        if (raceStored.getStatus() == null && raceDto.getStatus() != null) {
            raceStored.setStatus(raceDto.getStatus());
        }

        raceStored.getMapSiteUUID().put(site, raceDto.getId());

        raceRedisService.saveRace(raceId, raceStored).subscribe();
    }

    public void saveMeetingSite(List<Meeting> meetings, Integer site) {
        Flux<MeetingSite> newMeetingSite = Flux.fromIterable(meetings).flatMap(
                r -> {
                    Mono<Long> generalId = meetingRepository.getMeetingDiffName(
                                CommonUtils.checkDiffStateMeeting(r.getState()),
                                r.getRaceType(),
                                r.getAdvertisedDate()
                            )
                            .collectList()
                            .mapNotNull(m -> {
                                Meeting result = CommonUtils.checkDiffMeetingName(m, r.getName());
                                if (result == null) {
                                    return null;
                                } else {
                                    return result.getId();
                                }
                            });
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
                    Mono<Long> generalId = getRaceByTypeAndNumberAndRangeAdvertisedStart(RaceResponseMapper.toRaceDTO(race));
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
                        Mono<Long> generalId = getRaceByTypeAndNumberAndRangeAdvertisedStart(race);
                        return Flux.from(generalId).map(id -> {
                                    if (AppConstant.ZBET_SITE_ID.equals(site)) {
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

        return Flux.fromIterable(mapSiteRaceUUID.entrySet())
                .parallel().runOn(Schedulers.parallel())
                .map(entry ->
                        serviceLookup.forBean(ICrawlService.class, SiteEnum.getSiteNameById(entry.getKey()))
                                .getEntrantByRaceUUID(entry.getValue()))
                .sequential()
                .collectList()
                .map(listRaceNewData -> {

                    CrawlRaceData result = new CrawlRaceData();
                    Map<Integer, CrawlEntrantData> mapEntrants = new HashMap<>();
                    Map<Integer, String> mapRaceResult = new HashMap<>();

                    for (CrawlRaceData raceNewData : listRaceNewData) {
                        // Set race status
                        if (result.getStatus() == null && raceNewData.getStatus() != null) {
                            result.setStatus(raceNewData.getStatus());
                        }

                        // Set race final result
                        if (raceNewData.getFinalResult() != null) {
                            mapRaceResult.putAll(raceNewData.getFinalResult());
                        }

                        // Set entrants position and price
                        if (raceNewData.getMapEntrants() != null) {
                            raceNewData.getMapEntrants().forEach((entrantNumber, entrantNewData) -> {

                                if (mapEntrants.containsKey(entrantNumber)) {
                                    mapEntrants.get(entrantNumber).getPriceMap().putAll(entrantNewData.getPriceMap());
                                    if (raceNewData.getSiteId().equals(SiteEnum.ZBET.getId())) {
                                        mapEntrants.get(entrantNumber).setPosition(entrantNewData.getPosition());
                                        mapEntrants.get(entrantNumber).setIsScratched(entrantNewData.getIsScratched());
                                        mapEntrants.get(entrantNumber).setScratchTime(entrantNewData.getScratchTime());
                                    }
                                } else {
                                    mapEntrants.put(entrantNumber, entrantNewData);
                                }
                            });
                        }
                    }

                    result.setMapEntrants(mapEntrants);
                    result.setFinalResult(mapRaceResult);

                    return result;
                });
    }

    public void saveEntrantsPriceIntoDB(List<Entrant> newEntrant, RaceDto raceDto, Integer siteId) {

        Gson gson = new Gson();
        Flux<Entrant> existedFlux = getRaceByTypeAndNumberAndRangeAdvertisedStart(raceDto).flatMapMany(id -> entrantRepository.findByRaceId(id));
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
                .filter(m -> AppConstant.MARKETS_NAME.equals(m.getName())).findFirst()
                .orElseThrow(() -> new RuntimeException("No markets found"));

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

    public void updateRaceFinalResultIntoDB(RaceDto raceDto, Integer siteId, String finalResult) {
        Mono<Race> raceMono = raceRepository.getRaceByTypeAndNumberAndRangeAdvertisedStart(
                raceDto.getRaceType(),
                raceDto.getNumber(),
                raceDto.getAdvertisedStart().minus(30, ChronoUnit.MINUTES),
                raceDto.getAdvertisedStart().plus(30, ChronoUnit.MINUTES)
        ).collectList().mapNotNull(r -> CommonUtils.checkDiffRaceName(r, raceDto.getName()));
        raceMono.subscribe(race -> checkRaceFinalResultThenSave(race, finalResult, siteId));
    }

    public void updateRaceFinalResultIntoDB(Long raceId, Integer siteId, String finalResult) {
        raceRepository.findById(raceId).subscribe(race -> checkRaceFinalResultThenSave(race, finalResult, siteId));
    }

    private void checkRaceFinalResultThenSave(Race race, String finalResult, Integer siteId) {
        Gson gson = new Gson();
        Type type = new TypeToken<Map<Integer, String>>() {}.getType();

        Map<Integer, String> existedFinalResult;
        if (race.getResultsDisplay() != null) {
            existedFinalResult = gson.fromJson(race.getResultsDisplay().asString(), type);
        } else {
            existedFinalResult = new HashMap<>();
        }
        existedFinalResult.put(siteId, finalResult);

        raceRepository.updateRaceFinalResultById(Json.of(gson.toJson(existedFinalResult)), race.getId()).subscribe();
    }

    public Map<Integer, Integer> getPositionInResult(String input){
        String[] groups = input.split("/");
        Map<Integer, Integer> output = new HashMap<>();
        int currentIndex = 1;
        for (String group : groups) {
            String[] values = group.split(",");
            for (String value : values) {
                int intValue = Integer.parseInt(value);
                output.put(intValue, currentIndex);
            }
            currentIndex=currentIndex + values.length;
        }
        return output;
    }

    public Mono<Long> getRaceByTypeAndNumberAndRangeAdvertisedStart(RaceDto race){
        return raceRepository.getRaceByTypeAndNumberAndRangeAdvertisedStart(
                race.getRaceType(), race.getNumber(),
                race.getAdvertisedStart().minus(30, ChronoUnit.MINUTES),
                race.getAdvertisedStart().plus(30, ChronoUnit.MINUTES)
        ).collectList().mapNotNull(races -> {
            Race result = CommonUtils.checkDiffRaceName(races, race.getName());
            if (result == null) {
                return null;
            } else {
                return result.getId();
            }
        }) ;
    }


}
