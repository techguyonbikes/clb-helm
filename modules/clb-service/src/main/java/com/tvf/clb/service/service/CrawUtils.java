package com.tvf.clb.service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.dto.topsport.TopSportMeetingDto;
import com.tvf.clb.base.entity.*;
import com.tvf.clb.base.exception.ApiRequestFailedException;
import com.tvf.clb.base.model.*;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.CommonUtils;
import com.tvf.clb.service.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.*;
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

    private final FailedApiCallService failedApiCallService;

    private final ObjectMapper objectMapper;


    public CrawUtils(ServiceLookup serviceLookup, MeetingSiteRepository meetingSiteRepository,
                     RaceSiteRepository raceSiteRepository, MeetingRepository meetingRepository,
                     RaceRepository raceRepository, RaceRedisService raceRedisService,
                     EntrantRepository entrantRepository, FailedApiCallService failedApiCallService,
                     ObjectMapper objectMapper) {
        this.serviceLookup = serviceLookup;
        this.meetingSiteRepository = meetingSiteRepository;
        this.raceSiteRepository = raceSiteRepository;
        this.meetingRepository = meetingRepository;
        this.raceRepository = raceRepository;
        this.raceRedisService = raceRedisService;
        this.entrantRepository = entrantRepository;
        this.failedApiCallService = failedApiCallService;
        this.objectMapper = objectMapper;
    }

    public void saveEntrantCrawlDataToRedis(List<Entrant> entrants, RaceDto raceDto, Integer site) {
        Mono<RaceResponseDto> raceStoredMono = raceRedisService.findByRaceId(raceDto.getRaceId());
        raceStoredMono.subscribe(raceStored -> saveEntrantDataToRedis(entrants, site, raceStored, raceDto));
    }

    public void saveEntrantDataToRedis(List<Entrant> entrants, Integer site, RaceResponseDto raceStored, RaceDto raceDto){
        Map<Integer, Entrant> newEntrantMap = new HashMap<>();
        for (Entrant entrant : entrants) {
            newEntrantMap.put(entrant.getNumber(), entrant);
        }
        for (EntrantResponseDto entrantResponseDto : raceStored.getEntrants()) {
            Entrant newEntrant = newEntrantMap.get(entrantResponseDto.getNumber());
            Map<Integer, Float> winPriceDeductions = entrantResponseDto.getWinPriceDeductions();
            Map<Integer, Float> placePriceDeductions = entrantResponseDto.getPlacePriceDeductions();
            if (newEntrant != null) {
                checkPriceRawData(newEntrant.getCurrentSitePrice(), entrantResponseDto.getPriceFluctuations(), site);
                checkPriceRawData(newEntrant.getCurrentSitePricePlaces(), entrantResponseDto.getPricePlaces(), site);
                if (newEntrant.getCurrentWinDeductions() != null) {
                    winPriceDeductions.put(site, newEntrant.getCurrentWinDeductions());
                }
                if (newEntrant.getCurrentPlaceDeductions() != null) {
                    placePriceDeductions.put(site, newEntrant.getCurrentPlaceDeductions());
                }
            }
        }

        compareNewAndStoredRace(raceStored, raceDto, site);

        raceRedisService.saveRace(raceDto.getRaceId(), raceStored).subscribe();
    }

    public void checkPriceRawData(List<Float> currentSitePrice, Map<Integer, List<PriceHistoryData>> entrantPriceData, Integer site){
        List<PriceHistoryData> sitePrice = currentSitePrice == null ? new ArrayList<>() :
                CommonUtils.convertToPriceHistoryData(currentSitePrice);
        entrantPriceData.put(site, sitePrice);
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

    public Mono<Map<String, Long>> saveMeetingSiteAndRaceSite(Map<Meeting, List<Race>> mapMeetingAndRace, Integer site) {

        Flux<MeetingSite> newMeetingSiteFlux = Flux.empty();
        Flux<RaceSite> newRaceSiteFlux = Flux.empty();

        for (Map.Entry<Meeting, List<Race>> entry : mapMeetingAndRace.entrySet()) {
            Meeting newMeeting = entry.getKey();
            List<Race> newRaces = entry.getValue();

            checkMeetingWrongAdvertisedStart(newMeeting, newRaces);
            newRaces = removeDuplicateRaceNumber(newRaces);

            Mono<Long> meetingId = getMeetingIdAndCompareMeetingNames(newMeeting, newRaces, site).cache();
            newMeetingSiteFlux = newMeetingSiteFlux.concatWith(meetingId.map(id -> MeetingMapper.toMetingSite(newMeeting, site, id)));

            Flux<RaceSite> raceSites = getRaceSitesFromMeetingIdAndRaces(meetingId, newRaces, site).cache();
            newRaceSiteFlux = newRaceSiteFlux.concatWith(raceSites);
        }

        return saveMeetingSite(mapMeetingAndRace.keySet(), newMeetingSiteFlux, site)
                .thenMany(saveRaceSite(newRaceSiteFlux, site))
                .then(newRaceSiteFlux.collectMap(RaceSite::getRaceSiteId, RaceSite::getGeneralRaceId));
    }

    public Flux<MeetingSite> saveMeetingSite(Collection<Meeting> meetings, Flux<MeetingSite> newMeetingSiteFlux, Integer site) {
        Flux<MeetingSite> existedMeetingSiteFlux = meetingSiteRepository
                .findAllByMeetingSiteIdInAndSiteId(meetings.stream().map(Meeting::getMeetingId).collect(Collectors.toList()), site);

        return Flux.zip(newMeetingSiteFlux.collectList(), existedMeetingSiteFlux.collectList())
                .flatMap(tuple2 -> {
                    List<MeetingSite> newMeetingSites = new ArrayList<>(tuple2.getT1());
                    newMeetingSites.removeAll(tuple2.getT2());
                    log.info("Meeting site " + site + " need to be update is " + newMeetingSites.size());
                    return meetingSiteRepository.saveAll(newMeetingSites);
                });
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

    public Mono<Long> getMeetingIdAndCompareMeetingNames(Meeting newMeeting, List<Race> newRaces, Integer site){
        return meetingRepository.findAllMeetingByRaceTypeAndAdvertisedDate(
                        newMeeting.getRaceType(),
                        newMeeting.getAdvertisedDate().minus(30, ChronoUnit.MINUTES),
                        newMeeting.getAdvertisedDate().plus(30, ChronoUnit.MINUTES)
                )
                .collectList()
                .flatMap(existingMeetings -> {
                    List<Long> existingMeetingIds = existingMeetings.stream().map(Meeting::getId).collect(Collectors.toList());
                    Flux<Race> existingRaceFlux = raceRepository.findAllByMeetingIdIn(existingMeetingIds);

                    return existingRaceFlux.collectList()
                            .mapNotNull(existingRaces -> {
                                Map<Long, List<Race>> mapMeetingIdAndRaces = existingRaces.stream().collect(Collectors.groupingBy(Race::getMeetingId));

                                Map<Meeting, List<Race>> mapExistingMeetingAndRace = new HashMap<>();
                                existingMeetings.forEach(meeting -> mapExistingMeetingAndRace.put(meeting, mapMeetingIdAndRaces.get(meeting.getId())));

                                Meeting result = CommonUtils.mapNewMeetingToExisting(mapExistingMeetingAndRace, newMeeting, newRaces);
                                if (result == null) {
                                    log.info("[SaveMeetingSiteAndRaceSite] Can't map new meeting with name {} {} in site {}, race type = {}", newMeeting.getName(), newMeeting.getAdvertisedDate(), site, newMeeting.getRaceType());
                                    return null;
                                } else {
                                    return result.getId();
                                }
                            });
                });
    }

    public Flux<RaceSite> saveRaceSite(Flux<RaceSite> newRaceSiteFlux, Integer site) {
        return newRaceSiteFlux.collectList().flatMapMany(newRaceSites ->
                    raceSiteRepository
                            .findAllByGeneralRaceIdInAndSiteId(newRaceSites.stream().map(RaceSite::getGeneralRaceId).collect(Collectors.toList()), site)
                            .collectList()
                            .flatMapMany(existedRaceSites -> saveOrUpdateRaceSiteToDB(newRaceSites, existedRaceSites, site))
                );
    }

    private Flux<RaceSite> saveOrUpdateRaceSiteToDB(List<RaceSite> newRaceSites, List<RaceSite> existedRaceSites, Integer site) {
        List<RaceSite> raceSitesNeedToInsert = new ArrayList<>();

        Flux<RaceSite> updateRaceSites = Flux.empty();

        for (RaceSite newRaceSite : newRaceSites) {
            if (! existedRaceSites.contains(newRaceSite)) {
                raceSitesNeedToInsert.add(newRaceSite);
            } else {
                for (RaceSite existedRaceSite : existedRaceSites) {
                    if (existedRaceSite.equals(newRaceSite) && ! existedRaceSite.getRaceSiteId().equals(newRaceSite.getRaceSiteId())) {
                        updateRaceSites = updateRaceSites.concatWith(raceSiteRepository.updateRaceSiteIdAndRaceSiteUrl(newRaceSite.getRaceSiteId(), newRaceSite.getRaceSiteUrl(), existedRaceSite.getId()));
                        break;
                    }
                }
            }
        }

        log.info("Race site " + site + " need to be update is " + raceSitesNeedToInsert.size());
        return raceSiteRepository.saveAll(raceSitesNeedToInsert).thenMany(updateRaceSites);
    }

    public Mono<CrawlRaceData> crawlNewDataByRaceUUID(Map<Integer, String> mapSiteRaceUUID) {

        return Flux.fromIterable(mapSiteRaceUUID.entrySet())
                .flatMap(entry -> serviceLookup.forBean(ICrawlService.class, SiteEnum.getSiteNameById(entry.getKey()))
                                               .getEntrantByRaceUUID(entry.getValue()))
                .onErrorContinue((throwable, o) -> log.error("Got exception {} while crawling uuids {} caused by {}", throwable.getMessage(), mapSiteRaceUUID, o))
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
                        if (AppConstant.STATUS_ABANDONED.equals(raceNewData.getStatus())) {
                            mapRaceInterimResult.put(raceNewData.getSiteEnum().getId(), "");
                        }
                        setIfPresent(raceNewData.getFinalResult(), mapRaceFinalResult::putAll);

                        // Set race interim result
                        setIfPresent(raceNewData.getInterimResult(), mapRaceInterimResult::putAll);

                        // Set map entrants
                        setMapEntrantProperties(raceNewData, mapEntrants);

                        // Set advertised start
                        setIfPresent(raceNewData.getAdvertisedStart(), result::setAdvertisedStart);

                        //Set actual start
                        setIfPresent(raceNewData.getActualStart(), result::setActualStart);

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
                CrawlEntrantData existedEntrant = mapEntrants.get(entrantNumber);

                existedEntrant.getPriceMap().putAll(entrantNewData.getPriceMap());
                existedEntrant.getPricePlacesMap().putAll(entrantNewData.getPricePlacesMap());
                existedEntrant.getWinDeductions().putAll(entrantNewData.getWinDeductions());
                existedEntrant.getPlaceDeductions().putAll(entrantNewData.getPlaceDeductions());

                if (raceNewData.getSiteEnum().equals(SiteEnum.LAD_BROKE)) {
                    existedEntrant.setIsScratched(entrantNewData.getIsScratched());
                    existedEntrant.setScratchTime(entrantNewData.getScratchTime());
                }
            } else {
                mapEntrants.put(entrantNumber, entrantNewData);
            }
        });
    }

    public void saveEntrantsPriceIntoDB(List<Entrant> listNewEntrants, Long raceId, Integer siteId) {
        Flux<Entrant> existedFlux = entrantRepository.findByRaceId(raceId);
        List<Entrant> listNeedToUpdate = new ArrayList<>();
        existedFlux.collectList().subscribe(listExisted -> {

            Map<Integer, Entrant> mapNumberToNewEntrant = listNewEntrants.stream().collect(Collectors.toMap(Entrant::getNumber, Function.identity(), (first, second) -> first));
            listExisted.forEach(existed -> {

                Map<Integer, List<PriceHistoryData>> allExistedSitePrices = CommonUtils.getSitePriceFromJsonb(existed.getPriceFluctuations());
                List<PriceHistoryData> existedSitePrice = allExistedSitePrices.getOrDefault(siteId, new ArrayList<>());

                if (existed.getNumber() != null) {
                    Entrant newEntrant = mapNumberToNewEntrant.get(existed.getNumber());

                    if (newEntrant != null) {
                        List<PriceHistoryData> newSitePrice = CommonUtils.convertToPriceHistoryData(newEntrant.getCurrentSitePrice());
                        if (!CollectionUtils.isEmpty(newSitePrice) && !Objects.equals(existedSitePrice, newSitePrice)) {
                            allExistedSitePrices.put(siteId, newSitePrice);
                            existed.setPriceFluctuations(CommonUtils.toJsonb(allExistedSitePrices));
                        }
                        setEntrantPriceDeductions(existed, newEntrant, siteId);
                        listNeedToUpdate.add(existed);
                    }
                }
            });
            entrantRepository.saveAll(listNeedToUpdate).subscribe();
        });
    }

    private void setEntrantPriceDeductions(Entrant existed, Entrant newEntrant, Integer siteId) {
        Map<Integer, Float> allExistedWinDeductions = CommonUtils.getPriceFromJsonb(existed.getPriceWinDeductions());
        Map<Integer, Float> allExistedPlaceDeductions = CommonUtils.getPriceFromJsonb(existed.getPricePlaceDeductions());
        if (newEntrant.getCurrentWinDeductions() != null) {
            allExistedWinDeductions.put(siteId, newEntrant.getCurrentWinDeductions());
            existed.setPriceWinDeductions(CommonUtils.toJsonb(allExistedWinDeductions));
        }
        if (newEntrant.getCurrentPlaceDeductions() != null) {
            allExistedPlaceDeductions.put(siteId, newEntrant.getCurrentPlaceDeductions());
            existed.setPricePlaceDeductions(CommonUtils.toJsonb(allExistedPlaceDeductions));
        }
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

            if (meetingAdvertisedStart == null) {
                meeting.setAdvertisedDate(lastRaceAdvertisedStart.atZone(ZoneOffset.UTC).with(LocalTime.MIN).toInstant());
            } else if (lastRaceAdvertisedStart.atZone(ZoneOffset.UTC).with(LocalTime.MIN).isBefore(meetingAdvertisedStart.atZone(ZoneOffset.UTC).with(LocalTime.MIN))) {
                meeting.setAdvertisedDate(meetingAdvertisedStart.minus(1, ChronoUnit.DAYS));
            }
        }
    }

    private List<Race> removeDuplicateRaceNumber(List<Race> races) {
        races.sort(Comparator.comparing(Race::getNumber));
        Integer lastNumber = -1;
        List<Race> result = new ArrayList<>();
        for (Race race : races) {
            if (! race.getNumber().equals(lastNumber)) {
                result.add(race);
                lastNumber = race.getNumber();
            }
        }
        return result;
    }

    public void updateRaceFinalResultIntoDB(Long raceId, String finalResult, Integer siteId) {
        raceRepository.findById(raceId).subscribe(race -> checkRaceFinalResultThenSave(race, finalResult, siteId));
    }

    private void checkRaceFinalResultThenSave(Race race, String finalResult, Integer siteId) {
        Map<Integer, String> existedFinalResult = CommonUtils.getMapRaceFinalResultFromJsonb(race.getResultsDisplay());
        if (existedFinalResult == null) {
            existedFinalResult = new HashMap<>();
        }
        existedFinalResult.put(siteId, finalResult);

        raceRepository.updateRaceFinalResultById(CommonUtils.toJsonb(existedFinalResult), race.getId()).subscribe();
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

    public <T> Mono<T> crawlData(WebClient webClient, String uri, Class<T> returnType, String className, Long retryCrawl) throws ApiRequestFailedException {

        String simpleClassName = className.substring(className.lastIndexOf(".") + 1);

        Retry retryConfig = Retry.backoff(retryCrawl, Duration.ofSeconds(1))
                .doBeforeRetry(retrySignal -> log.info("[{}] Got exception \"{}\" while crawling data (uri = {}), retry attempt {}", simpleClassName, retrySignal.failure().getMessage(), uri, retrySignal.totalRetries() + 1))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> new ApiRequestFailedException(String.format("[%s] Retry exhausted (uri = %s)", simpleClassName, uri)));

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .<T>handle((bodyString, sink) -> {
                    try {
                        if (Document.class.isAssignableFrom(returnType)) {
                            sink.next(returnType.cast(Jsoup.parse(bodyString)));
                        } else {
                            sink.next(objectMapper.readValue(bodyString, returnType));
                        }
                    } catch (JsonProcessingException e) {
                        sink.error(new ApiRequestFailedException(String.format("Can not map body to type %s with exception detail: %s", returnType.getSimpleName(), e.getMessage())));
                    }
                })
                .retryWhen(retryConfig)
                .doOnError(throwable -> log.error("[{}] Crawling data (uri = {}) failed after {} retries", simpleClassName, uri, retryCrawl));
    }

    public Mono<Long> getIdForNewRaceAndSaveRaceSite(RaceDto newRace, List<Entrant> newEntrants, Integer siteId) {
        return this.getIdForNewRace(newRace, newEntrants)
                   .flatMap(raceId -> {
                       log.info("Update race site {} with uuid = {}, race name = {}, race number = {} meeting name = {}", siteId, newRace.getId(), newRace.getName(), newRace.getNumber(), newRace.getMeetingName());
                       return this.saveRaceSite(Flux.just(RaceResponseMapper.toRaceSiteDto(newRace, siteId, raceId)), siteId)
                                  .then(Mono.just(raceId));
                   });
    }

    public Mono<Long> getIdForNewRace(RaceDto newRace, List<Entrant> newEntrants){
        return raceRepository.getRaceByTypeAndNumberAndRangeAdvertisedStart(
                newRace.getRaceType(), newRace.getNumber(),
                newRace.getAdvertisedStart().minus(30, ChronoUnit.MINUTES),
                newRace.getAdvertisedStart().plus(30, ChronoUnit.MINUTES)
        ).collectList().flatMap(existingRaces ->
                entrantRepository.findByRaceIdIn(existingRaces.stream().map(Race::getId).collect(Collectors.toList()))
                                 .collectList().mapNotNull(entrants -> {
                                     Map<Long, List<Entrant>> mapRaceIdAndEntrants = entrants.stream().collect(Collectors.groupingBy(Entrant::getRaceId));
                                     Map<Race, List<Entrant>> mapExistingRaceAndEntrants = new HashMap<>();
                                     existingRaces.forEach(race -> mapExistingRaceAndEntrants.put(race, mapRaceIdAndEntrants.get(race.getId())));
                                     Race result = CommonUtils.mapNewRaceToExisting(mapExistingRaceAndEntrants, newRace, newEntrants);
                                     if (result == null) {
                                         return null;
                                     } else {
                                         return result.getId();
                                     }
                                 })) ;
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
