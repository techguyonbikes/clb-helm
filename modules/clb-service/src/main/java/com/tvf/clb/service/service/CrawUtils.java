package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.dto.topsport.TopSportMeetingDto;
import com.tvf.clb.base.entity.*;
import com.tvf.clb.base.exception.ApiRequestFailedException;
import com.tvf.clb.base.model.*;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.CommonUtils;
import com.tvf.clb.service.repository.*;
import io.r2dbc.postgresql.codec.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tvf.clb.base.utils.AppConstant.MAX_RETRIES;
import static com.tvf.clb.base.utils.AppConstant.RETRY_DELAY_TIME;
import static com.tvf.clb.base.utils.CommonUtils.setIfPresent;

@Service
@Slf4j
public class CrawUtils {

    private final ServiceLookup serviceLookup;

    private final MeetingSiteRepository meetingSiteRepository;

    private final RaceSiteRepository raceSiteRepository;

    private final MeetingRepository meetingRepository;

    private final RaceRepository raceRepository;

    private final RaceRedisService raceRedisService;

    private final EntrantRepository entrantRepository;

    private final ReactiveRedisTemplate<String, Long> raceNameAndIdTemplate;

    private final FailedApiCallService failedApiCallService;


    public CrawUtils(ServiceLookup serviceLookup, MeetingSiteRepository meetingSiteRepository,
                     RaceSiteRepository raceSiteRepository, MeetingRepository meetingRepository,
                     RaceRepository raceRepository, RaceRedisService raceRedisService,
                     EntrantRepository entrantRepository, ReactiveRedisTemplate<String, Long> raceNameAndIdTemplate,
                     FailedApiCallService failedApiCallService) {
        this.serviceLookup = serviceLookup;
        this.meetingSiteRepository = meetingSiteRepository;
        this.raceSiteRepository = raceSiteRepository;
        this.meetingRepository = meetingRepository;
        this.raceRepository = raceRepository;
        this.raceRedisService = raceRedisService;
        this.entrantRepository = entrantRepository;
        this.raceNameAndIdTemplate = raceNameAndIdTemplate;
        this.failedApiCallService = failedApiCallService;
    }

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
                Map<Integer, List<PriceHistoryData>> price = new HashMap<>();
                log.error("Entrant id = {} in race id = {} null price", entrantResponseDto.getId(), raceStored.getId());
                entrantResponseDto.setPriceFluctuations(price);
            }

            if (newEntrant != null) {
                Map<Integer, List<PriceHistoryData>> price;
                if(entrantResponseDto.getPriceFluctuations() != null) {
                    price = entrantResponseDto.getPriceFluctuations();
                    List<PriceHistoryData> sitePrice = newEntrant.getCurrentSitePrice() == null ? new ArrayList<>() :
                            CommonUtils.convertToPriceHistoryData(newEntrant.getCurrentSitePrice());
                    price.put(site, sitePrice);
                }
            }
        }

        compareNewAndStoredRace(raceStored, raceDto, site);

        raceRedisService.saveRace(raceId, raceStored).subscribe();
    }

    public void compareNewAndStoredRace(RaceResponseDto raceStored, RaceDto raceDto, Integer site){
        if (raceStored == null || raceDto == null || site == null)
            return;

        if (raceStored.getStatus() == null && raceDto.getStatus() != null) {
            raceStored.setStatus(raceDto.getStatus());
        }

        if (raceDto.getFinalResult() != null) {
            raceStored.getFinalResult().put(site, raceDto.getFinalResult());
        }
        if (raceDto.getRaceSiteUrl() != null && raceStored.getRaceSiteUrl() !=null) {
            if(site == 2){
                raceStored.getRaceSiteUrl().put(site, raceDto.getRaceSiteUrl().replace("ladbrokes","neds"));
            }
            else {
                raceStored.getRaceSiteUrl().put(site, raceDto.getRaceSiteUrl());
            }
        }

        raceStored.getMapSiteUUID().put(site, raceDto.getId());
    }

    public void saveMeetingSiteAndRaceSite(Map<Meeting, List<Race>> mapMeetingAndRace, Integer site) {

        Flux<MeetingSite> newMeetingSiteFlux = Flux.empty();
        Flux<RaceSite> newRaceSiteFlux = Flux.empty();

        for (Map.Entry<Meeting, List<Race>> entry : mapMeetingAndRace.entrySet()) {
            Meeting newMeeting = entry.getKey();
            List<Race> newRaces = entry.getValue();

            checkMeetingWrongAdvertisedStart(newMeeting, newRaces);

            Mono<Long> meetingId = getMeetingIdAndCompareMeetingNames(newMeeting, site);

            newMeetingSiteFlux = newMeetingSiteFlux.concatWith(meetingId.map(id -> MeetingMapper.toMetingSite(newMeeting, site, id)));

            Flux<RaceSite> raceSites = getRaceSitesFromMeetingIdAndRaces(meetingId, newRaces, site);
            newRaceSiteFlux = newRaceSiteFlux.concatWith(raceSites);
        }

        saveMeetingSite(mapMeetingAndRace.keySet(), newMeetingSiteFlux, site);
        saveRaceSite(newRaceSiteFlux, site);
    }

    public void saveMeetingSite(Collection<Meeting> meetings, Flux<MeetingSite> newMeetingSiteFlux, Integer site) {
        Flux<MeetingSite> existedMeetingSiteFlux = meetingSiteRepository
                .findAllByMeetingSiteIdInAndSiteId(meetings.stream().map(Meeting::getMeetingId).collect(Collectors.toList()), site);

        Flux.zip(newMeetingSiteFlux.collectList(), existedMeetingSiteFlux.collectList())
                .flatMap(tuple2 -> {
                    List<MeetingSite> newMeetingSites = new ArrayList<>(tuple2.getT1());
                    newMeetingSites.removeAll(tuple2.getT2());
                    log.info("Meeting site " + site + " need to be update is " + newMeetingSites.size());
                    return meetingSiteRepository.saveAll(newMeetingSites);
                })
                .subscribe();
    }

    public Flux<RaceSite> getRaceSitesFromMeetingIdAndRaces(Mono<Long> meetingId, List<Race> newRaces, Integer site) {
        return meetingId.flatMapMany(raceRepository::findAllByMeetingId)
                        .collectList()
                        .flatMapIterable(existingRaces -> {
                            Map<Integer, Long> mapRaceNumberAndId = existingRaces.stream().collect(Collectors.toMap(Race::getNumber, Race::getId));
                            List<RaceSite> raceSiteList = new ArrayList<>();

                            for (Race newRace : newRaces) {
                                if (mapRaceNumberAndId.containsKey(newRace.getNumber())) {
                                    Long raceId = mapRaceNumberAndId.get(newRace.getNumber());
                                    raceSiteList.add(RaceResponseMapper.toRacesiteDto(newRace, site, raceId));
                                }
                            }
                            return raceSiteList;
                        });
    }

    public Mono<Long> getMeetingIdAndCompareMeetingNames(Meeting newMeeting, Integer site){
        return meetingRepository.findAllMeetingByRaceTypeAndAdvertisedDate(
                        newMeeting.getRaceType(),
                        newMeeting.getAdvertisedDate().minus(30, ChronoUnit.MINUTES),
                        newMeeting.getAdvertisedDate().plus(30, ChronoUnit.MINUTES)
                )
                .collectList()
                .mapNotNull(m -> {
                    Meeting result = CommonUtils.getMeetingDiffMeetingName(m, newMeeting.getName());
                    if (result == null) {
                        log.info("[SaveMeetingSiteAndRaceSite] Can't map new meeting with name {} in site {}", newMeeting.getName(), site);
                        return null;
                    } else {
                        return result.getId();
                    }
                });
    }

    public void saveRaceSite(Flux<RaceSite> newRaceSiteFlux, Integer site) {
        newRaceSiteFlux.collectList()
                .subscribe(newRaceSites -> raceSiteRepository
                        .findAllByGeneralRaceIdInAndSiteId(newRaceSites.stream().map(RaceSite::getGeneralRaceId).collect(Collectors.toList()), site)
                        .collectList()
                        .subscribe(existedRaceSites -> saveOrUpdateRaceSiteToDB(newRaceSites, existedRaceSites, site)));
    }

    private void saveOrUpdateRaceSiteToDB(List<RaceSite> newRaceSites, List<RaceSite> existedRaceSites, Integer site) {
        List<RaceSite> raceSitesNeedToInsert = new ArrayList<>();

        for (RaceSite newRaceSite : newRaceSites) {
            if (! existedRaceSites.contains(newRaceSite)) {
                raceSitesNeedToInsert.add(newRaceSite);
            } else {
                existedRaceSites.stream()
                        .filter(existed -> existed.getSiteId().equals(site) && existed.getGeneralRaceId().equals(newRaceSite.getGeneralRaceId()))
                        .findFirst()
                        .ifPresent(existed -> {
                            if (! existed.getRaceSiteId().equals(newRaceSite.getRaceSiteId())) {
                                raceSiteRepository.updateRaceSiteIdAndRaceSiteUrl(newRaceSite.getRaceSiteId(), newRaceSite.getRaceSiteUrl(), existed.getId()).subscribe();
                            }
                        });
            }
        }

        log.info("Race site " + site + " need to be update is " + raceSitesNeedToInsert.size());
        raceSiteRepository.saveAll(raceSitesNeedToInsert).subscribe();
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
                    Map<Integer, String> mapRaceFinalResult = new HashMap<>();
                    Map<Integer, String> mapRaceInterimResult = new HashMap<>();

                    CrawlRaceData racePriorData = null;

                    for (CrawlRaceData raceNewData : listRaceNewData) {
                        if (raceNewData.getSiteEnum() == null) continue;

                        // Set race final result
                        setIfPresent(raceNewData.getFinalResult(), mapRaceFinalResult::putAll);

                        // Set race interim result
                        setIfPresent(raceNewData.getInterimResult(), mapRaceInterimResult::putAll);

                        // Set map entrants
                        setMapEntrantProperties(raceNewData, mapEntrants);

                        // Set advertised start
                        setIfPresent(raceNewData.getAdvertisedStart(), result::setAdvertisedStart);

                        if (racePriorData == null || racePriorData.getSiteEnum().getStatusPriority() > raceNewData.getSiteEnum().getStatusPriority()) {
                            racePriorData = raceNewData;
                        }
                    }

                    if (racePriorData != null){
                        result.setStatus(racePriorData.getStatus());
                        racePriorData.getMapEntrants().forEach((entrantNumber, entrantNewData) ->
                                mapEntrants.get(entrantNumber).setPosition(entrantNewData.getPosition()));
                    }


                    result.setMapEntrants(mapEntrants);
                    result.setFinalResult(mapRaceFinalResult);
                    result.setInterimResult(mapRaceInterimResult);

                    return result;
                });
    }

    public void setMapEntrantProperties(CrawlRaceData raceNewData, Map<Integer, CrawlEntrantData> mapEntrants) {
        if (raceNewData == null || raceNewData.getMapEntrants() == null) {
            return;
        }
        raceNewData.getMapEntrants().forEach((entrantNumber, entrantNewData) -> {
            if (mapEntrants.containsKey(entrantNumber)) {
                mapEntrants.get(entrantNumber).getPriceMap().putAll(entrantNewData.getPriceMap());
                if (raceNewData.getSiteEnum().equals(SiteEnum.LAD_BROKE)) {
                    mapEntrants.get(entrantNumber).setIsScratched(entrantNewData.getIsScratched());
                    mapEntrants.get(entrantNumber).setScratchTime(entrantNewData.getScratchTime());
                }
            } else {
                mapEntrants.put(entrantNumber, entrantNewData);
            }
        });
    }

    public void saveEntrantsPriceIntoDB(List<Entrant> newEntrant, RaceDto raceDto, Integer siteId) {

        Gson gson = new Gson();
        Flux<Entrant> existedFlux = getRaceByTypeAndNumberAndRangeAdvertisedStart(raceDto).flatMapMany(entrantRepository::findByRaceId);
        List<Entrant> listNeedToUpdate = new ArrayList<>();
        existedFlux.collectList().subscribe(listExisted -> {

                Map<Integer, Entrant> mapNumberToNewEntrant = newEntrant.stream().collect(Collectors.toMap(Entrant::getNumber, Function.identity(), (first, second) -> first));
                listExisted.forEach(existed -> {

                        Map<Integer, List<PriceHistoryData>> allExistedSitePrices = CommonUtils.getSitePriceFromJsonb(existed.getPriceFluctuations());

                        List<PriceHistoryData> existedSitePrice = allExistedSitePrices.getOrDefault(siteId, new ArrayList<>());
                        List<PriceHistoryData> newSitePrice = new ArrayList<>();
                        if (existed.getNumber() != null) {
                            Entrant entrant = mapNumberToNewEntrant.get(existed.getNumber());
                            newSitePrice = CommonUtils.convertToPriceHistoryData(entrant == null ? null : entrant.getCurrentSitePrice());
                        }
                        if (!CollectionUtils.isEmpty(newSitePrice) && !Objects.equals(existedSitePrice, newSitePrice)) {
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

        if (marketsRawData.getEntrantIds() != null) {
            marketsRawData.getEntrantIds().forEach(x -> {
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
        }

        return result;
    }

    public void checkMeetingWrongAdvertisedStart(MeetingRawData meeting, List<RaceRawData> races) {
        Optional<RaceRawData> lastRace = races.stream().max(Comparator.comparing(RaceRawData::getAdvertisedStart));

        if (lastRace.isPresent()) {
            Instant lastRaceAdvertisedStart = Instant.parse(lastRace.get().getAdvertisedStart());
            Instant meetingAdvertisedStart = Instant.parse(meeting.getAdvertisedDate());

            if (lastRaceAdvertisedStart.atZone(ZoneOffset.UTC).with(LocalTime.MIN).isBefore(meetingAdvertisedStart.atZone(ZoneOffset.UTC).with(LocalTime.MIN))) {
                meeting.setAdvertisedDate(meetingAdvertisedStart.minus(1, ChronoUnit.DAYS).toString());
            }
        }
    }

    public void checkMeetingWrongAdvertisedStart(Meeting meeting, List<Race> races) {
        Optional<Race> lastRace = races.stream().max(Comparator.comparing(Race::getAdvertisedStart));

        if (lastRace.isPresent()) {
            Instant lastRaceAdvertisedStart = lastRace.get().getAdvertisedStart();
            Instant meetingAdvertisedStart = meeting.getAdvertisedDate();

            if (lastRaceAdvertisedStart.atZone(ZoneOffset.UTC).with(LocalTime.MIN).isBefore(meetingAdvertisedStart.atZone(ZoneOffset.UTC).with(LocalTime.MIN))) {
                meeting.setAdvertisedDate(meetingAdvertisedStart.minus(1, ChronoUnit.DAYS));
            }
        }
    }

    public void updateRaceFinalResultIntoDB(RaceDto raceDto, Integer siteId, String finalResult) {
        Mono<Race> raceMono = raceRepository.getRaceByTypeAndNumberAndRangeAdvertisedStart(
                raceDto.getRaceType(),
                raceDto.getNumber(),
                raceDto.getAdvertisedStart().minus(30, ChronoUnit.MINUTES),
                raceDto.getAdvertisedStart().plus(30, ChronoUnit.MINUTES)
        ).collectList().mapNotNull(r -> CommonUtils.getRaceDiffRaceName(r, raceDto.getName(), raceDto.getAdvertisedStart()));
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
        if (input == null) {
            return Collections.emptyMap();
        }
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

    public Flux<MeetingDto> crawlMeeting(CrawlMeetingFunction crawlFunction, LocalDate date, Long delayCrawlTime, String className)  {

        String simpleClassName = className.substring(className.lastIndexOf(".") + 1);

        int retryCount = 0;
        while (retryCount <= MAX_RETRIES) {
            try {
                Thread.sleep(delayCrawlTime);
                List<MeetingDto> meetingDtoList = crawlFunction.crawl(date);
                if (meetingDtoList != null) {
                    return Flux.fromIterable(meetingDtoList);
                }
                log.info("[{}] Got null data while crawl meetings {}, retry attempt {}", simpleClassName, date.toString(), retryCount + 1);
            } catch (InterruptedException interruptedException) {
                log.warn("[{}] Got InterruptedException {} while crawl meeting data", simpleClassName, interruptedException.getMessage());
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.info("[{}] Got exception \"{}\" while crawl meetings data {}, retry attempt {}", simpleClassName, e.getMessage(), date.toString(), retryCount + 1);
            }
            retryCount++;
            delayCrawlTime = RETRY_DELAY_TIME * retryCount;
        }

        log.error("[{}] Crawling meetings data {} failed after {} retries", simpleClassName, date.toString(), MAX_RETRIES);
        this.saveFailedCrawlMeeting(className, date);

        throw new ApiRequestFailedException(String.format("Got exception while crawling %s %s meetings", simpleClassName, date));
    }

    public Object crawlRace(CrawlRaceFunction crawlFunction, String raceUUID, String className) {

        String simpleClassName = className.substring(className.lastIndexOf(".") + 1);

        int retryCount = 0;

        while (retryCount <= MAX_RETRIES) {
            try {
                Object raceRawData = crawlFunction.crawl(raceUUID);
                if (raceRawData != null) {
                    return raceRawData;
                }
                log.info("[{}] Got null data while crawl race (uuid = {}), retry attempt {}", simpleClassName, raceUUID, retryCount + 1);
            } catch (Exception e) {
                log.info("[{}] Got exception \"{}\" while crawl race data (uuid = {}), retry attempt {}", simpleClassName, e.getMessage(), raceUUID, retryCount + 1);
            }
            ++ retryCount;
        }
        log.error("[{}] Crawling race data (uuid = {}) failed after {} retries", simpleClassName, raceUUID, MAX_RETRIES);

        return null;
    }

    public Mono<Long> getRaceByTypeAndNumberAndRangeAdvertisedStart(RaceDto race){
        return raceRepository.getRaceByTypeAndNumberAndRangeAdvertisedStart(
                race.getRaceType(), race.getNumber(),
                race.getAdvertisedStart().minus(30, ChronoUnit.MINUTES),
                race.getAdvertisedStart().plus(30, ChronoUnit.MINUTES)
        ).collectList().mapNotNull(races -> {
            Race result = CommonUtils.getRaceDiffRaceName(races, race.getName(), race.getAdvertisedStart());
            if (result == null) {
                return null;
            } else {
                return result.getId();
            }
        }) ;
    }

    public void saveFailedCrawlMeeting(String className, LocalDate crawlDate) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put(LocalDate.class.getName(), new Gson().toJson(crawlDate));
        failedApiCallService.saveFailedApiCallInfoToDB(className, "getTodayMeetings", params);
    }

    public void saveFailedCrawlRace(String className, RaceDto raceDto, LocalDate crawlTime) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put(RaceDto.class.getName(), new Gson().toJson(raceDto));
        params.put(LocalDate.class.getName(), new Gson().toJson(crawlTime));
        failedApiCallService.saveFailedApiCallInfoToDB(className, "crawlAndSaveEntrantsInRace", params);
    }

    public void saveFailedCrawlRaceForTopSport(TopSportMeetingDto meetingDto, String raceId, LocalDate crawlTime) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put(TopSportMeetingDto.class.getName(), new Gson().toJson(meetingDto));
        params.put(String.class.getName(), new Gson().toJson(raceId));
        params.put(LocalDate.class.getName(), new Gson().toJson(crawlTime));
        failedApiCallService.saveFailedApiCallInfoToDB(TopSportCrawlService.class.getName(), "crawlAndSaveEntrantsAndRace", params);
    }

}
