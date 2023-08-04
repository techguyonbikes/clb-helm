package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.dto.kafka.KafkaDtoMapper;
import com.tvf.clb.base.entity.*;
import com.tvf.clb.base.exception.ApiRequestFailedException;
import com.tvf.clb.base.kafka.payload.EventTypeEnum;
import com.tvf.clb.base.kafka.payload.KafkaPayload;
import com.tvf.clb.base.kafka.service.CloudbetKafkaService;
import com.tvf.clb.base.model.*;
import com.tvf.clb.base.model.ladbrokes.*;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.CommonUtils;
import com.tvf.clb.base.utils.ConvertBase;
import com.tvf.clb.base.utils.ExcelUtils;
import com.tvf.clb.service.repository.EntrantRepository;
import com.tvf.clb.service.repository.MeetingRepository;
import com.tvf.clb.service.repository.RaceRepository;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tvf.clb.base.utils.CommonUtils.setIfPresent;

@ClbService(componentType = AppConstant.LAD_BROKE)
@Slf4j
@AllArgsConstructor
public class LadBrokeCrawlService implements ICrawlService {

    private MeetingRepository meetingRepository;
    private RaceRepository raceRepository;
    private EntrantRepository entrantRepository;
    private CrawUtils crawUtils;
    private ServiceLookup serviceLookup;
    private RaceRedisService raceRedisService;
    private TodayData todayData;
    private CloudbetKafkaService kafkaService;
    private WebClient ladbrokesWebClient;

    @Autowired
    private ExcelUtils excelUtils;

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {
        String meetingQueryURI = AppConstant.LAD_BROKES_IT_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
        String className = this.getClass().getName();

        return crawUtils.crawlData(ladbrokesWebClient, meetingQueryURI, LadBrokedItMeetingDto.class, className, 5L)
                        .doOnError(throwable -> crawUtils.saveFailedCrawlMeeting(className, date))
                        .doOnNext(ladBrokedItMeetingDto -> todayData.setLastTimeCrawl(Instant.now()))
                        .flatMapIterable(ladBrokedItMeetingDto -> getAllAusMeeting(ladBrokedItMeetingDto, date));
    }

    @Override
    public Mono<CrawlRaceData> getEntrantByRaceUUID(String raceId) {
        Mono<LadbrokesRaceApiResponse> ladbrokesRaceApiResponseMono = getLadBrokedItRaceDto(raceId);

        return ladbrokesRaceApiResponseMono
                .onErrorResume(throwable -> Mono.empty())
                .filter(apiResponse -> apiResponse.getData() != null && apiResponse.getData().getRaces() != null)
                .map(LadbrokesRaceApiResponse::getData)
                .map(raceDto -> {
                    Map<String, LadbrokesRaceResult> results = raceDto.getResults();
                    Map<String, Integer> positions = new HashMap<>();
                    if (results != null) {
                        positions = results.keySet().stream().collect(Collectors.toMap(Function.identity(), key -> results.get(key).getPosition()));
                    } else {
                        positions.put(AppConstant.POSITION, 0);
                    }
                    HashMap<String, ArrayList<Float>> allEntrantPrices = raceDto.getPriceFluctuations();
                    HashMap<String, LadBrokesPriceOdds> allEntrantPricesPlaces = raceDto.getPricePlaces();
                    List<EntrantRawData> allEntrant = CommonUtils.getListEntrant(raceDto, allEntrantPrices, allEntrantPricesPlaces, raceId, positions);

                    Map<Integer, CrawlEntrantData> entrantMap = new HashMap<>();
                    allEntrant.forEach(x -> entrantMap.put(x.getNumber(), EntrantMapper.toCrawlEntrantData(x, AppConstant.LAD_BROKE_SITE_ID)));

                    CrawlRaceData result = new CrawlRaceData();
                    result.setSiteEnum(SiteEnum.LAD_BROKE);
                    result.setMapEntrants(entrantMap);
                    result.setAdvertisedStart(raceDto.getRaces().get(raceId).getAdvertisedStart());
                    result.setActualStart(raceDto.getRaces().get(raceId).getActualStart());

                    Optional<String> optionalStatus = getStatusFromRaceMarket(raceDto.getMarkets());

                    if (optionalStatus.isPresent()) {
                        String status = optionalStatus.get();
                        result.setStatus(status);
                        if (status.equals(AppConstant.STATUS_FINAL)) {
                            String top4Entrants = getWinnerEntrants(allEntrant).map(entrant -> String.valueOf(entrant.getNumber()))
                                    .collect(Collectors.joining(","));
                            result.setFinalResult(Collections.singletonMap(AppConstant.LAD_BROKE_SITE_ID, top4Entrants));
                        }
                    }

                    return result;
                });
    }

    private List<MeetingDto> getAllAusMeeting(LadBrokedItMeetingDto ladBrokedItMeetingDto, LocalDate date) {
        Collection<VenueRawData> venues = ladBrokedItMeetingDto.getVenues().values();
        List<String> venuesId = venues.stream().map(VenueRawData::getId).collect(Collectors.toList());

        Map<String, VenueRawData> meetingVenue = venues.stream().collect(Collectors.toMap(VenueRawData::getId, Function.identity()));

        List<MeetingRawData> meetings = ladBrokedItMeetingDto.getMeetings().values().stream()
                                                .filter(m -> venuesId.contains(m.getVenueId()) && m.getTrackCondition() != null)
                                                .collect(Collectors.toList());

        meetings.forEach(meeting -> {
            meeting.setState(meetingVenue.get(meeting.getVenueId()).getState());
            meeting.setCountry(meetingVenue.get(meeting.getVenueId()).getCountry());
        });

        List<String> raceIds = meetings.stream().map(MeetingRawData::getRaceIds).flatMap(List::stream).collect(Collectors.toList());

        List<RaceRawData> races = ladBrokedItMeetingDto.getRaces().values().stream().filter(r -> raceIds.contains(r.getId()) && r.getNumber() != null).collect(Collectors.toList());
        List<MeetingDto> meetingDtoList = new ArrayList<>();

        for (MeetingRawData localMeeting : meetings) {
            List<RaceRawData> localRace = races.stream().filter(r -> localMeeting.getRaceIds().contains(r.getId())).collect(Collectors.toList());

            crawUtils.checkMeetingWrongAdvertisedStart(localMeeting, localRace);

            MeetingDto meetingDto = MeetingMapper.toMeetingDto(localMeeting, localRace);
            meetingDtoList.add(meetingDto);
        }

        List<RaceDto> raceDtoList = meetingDtoList.stream().map(MeetingDto::getRaces).flatMap(List::stream).collect(Collectors.toList());
        saveMeetingAndRace(meetings, raceDtoList, date);

        return meetingDtoList;
    }

    private Optional<String> getStatusFromRaceMarket(Map<String, LadbrokesMarketsRawData> marketsRawDataMap) {
        Optional<LadbrokesMarketsRawData> finalFieldMarket = marketsRawDataMap.values().stream().filter(market -> market.getName().equals(AppConstant.MARKETS_NAME)).findFirst();
        return finalFieldMarket.flatMap(market -> ConvertBase.getLadbrokeRaceStatus(market.getMarketStatusId()));
    }

    private Stream<EntrantRawData> getWinnerEntrants(List<EntrantRawData> entrants) {
        return entrants.stream().parallel()
                .filter(entrant -> entrant.getPosition() > 0)
                .sorted(Comparator.comparing(EntrantRawData::getPosition))
                .limit(4);
    }

    private void saveMeetingAndRace(List<MeetingRawData> meetingRawData, List<RaceDto> raceDtoList, LocalDate date) {
        List<Meeting> newMeetings = meetingRawData.stream().map(MeetingMapper::toMeetingEntity).collect(Collectors.toList());
        List<String> uuidList = newMeetings.stream().map(Meeting::getMeetingId).collect(Collectors.toList());
        Flux<MeetingAndSiteUUID> existingMeetingFlux = meetingRepository.findAllByMeetingUUIDInAndSiteId(uuidList, SiteEnum.LAD_BROKE.getId());

        existingMeetingFlux.collectList()
                    .map(existingMeetings -> {
                        Map<String, Meeting> mapUUIDToMeeting = existingMeetings.stream().collect(Collectors.toMap(MeetingAndSiteUUID::getSiteUUID, Function.identity()));

                        List<Meeting> meetingsNeedUpdateOrInsert = new ArrayList<>();
                        newMeetings.forEach(newMeeting -> {
                            String newMeetingUUID = newMeeting.getMeetingId();
                            if (! mapUUIDToMeeting.containsKey(newMeetingUUID)) {
                                meetingsNeedUpdateOrInsert.add(newMeeting);
                            } else {
                                newMeeting.setId(mapUUIDToMeeting.get(newMeetingUUID).getId());
                            }
                        });
                        log.info("Meeting need to be update or insert {}", meetingsNeedUpdateOrInsert.size());
                        return meetingsNeedUpdateOrInsert;
                    })
                    .flatMap(meetingsNeedUpdateOrInsert -> Flux.fromIterable(meetingsNeedUpdateOrInsert)
                                                               .flatMap(meeting -> meetingRepository.save(meeting))
                                                               .collectList()
                    )
                    .flatMapMany(savedMeetings -> {
                        savedMeetings.stream().map(KafkaDtoMapper::convertToKafkaMeetingDto).forEach(meeting -> {
                            KafkaPayload payload = new KafkaPayload.Builder().eventType(EventTypeEnum.GENERIC).actualPayload((new Gson().toJson(meeting))).build();
                            kafkaService.publishKafka(payload, meeting.getId().toString(), null);
                        });

                        List<MeetingSite> meetingSites = savedMeetings.stream().map(meeting -> MeetingMapper.toMetingSite(meeting, SiteEnum.LAD_BROKE.getId(), meeting.getId())).collect(Collectors.toList());
                        return crawUtils.saveMeetingSite(savedMeetings, Flux.fromIterable(meetingSites), SiteEnum.LAD_BROKE.getId())
                                        .doOnComplete(() -> log.info("All meetings processed successfully"));
                    })
                    .then(Mono.defer(() -> saveRace(raceDtoList, newMeetings, date)))
                    .subscribe();
    }

    public Mono<Void> saveRace(List<RaceDto> raceDtoList, List<Meeting> meetings, LocalDate date) {
        Map<String, Meeting> meetingUUIDMap = meetings.stream()
                                                    .filter(meeting -> meeting.getMeetingId() != null)
                                                    .collect(Collectors.toMap(Meeting::getMeetingId, Function.identity(), (first, second) -> first));
        excelUtils.setVenueIdForRaces(raceDtoList);
        List<Race> newRaces = raceDtoList.stream().map(MeetingMapper::toRaceEntity).filter(x -> x.getNumber() != null).collect(Collectors.toList());
        List<Integer> raceNumbers = newRaces.stream().map(Race::getNumber).collect(Collectors.toList());
        List<Long> meetingIds = meetings.stream().map(Meeting::getId).collect(Collectors.toList());
        Flux<Race> existedRaces = raceRepository.findAllByNumberInAndMeetingIdIn(raceNumbers, meetingIds);

        return existedRaces.collectList()
                    .map(existed -> {
                        Map<String, Race> raceMeetingIdNumberMap = existed.stream()
                                .collect(Collectors.toMap(race -> race.getMeetingId() + " " + race.getNumber(), Function.identity()));
                        List<Race> raceNeedUpdateOrInsert = new ArrayList<>();

                        newRaces.forEach(newRace -> {
                            String key = meetingUUIDMap.get(newRace.getMeetingUUID()).getId() + " " + newRace.getNumber();
                            if (raceMeetingIdNumberMap.containsKey(key)) {
                                Race existing = raceMeetingIdNumberMap.get(key);
                                setIfPresent(newRace.getName(), existing::setName);
                                setIfPresent(newRace.getAdvertisedStart(), existing::setAdvertisedStart);
                                setIfPresent(newRace.getActualStart(), existing::setActualStart);
                                setIfPresent(newRace.getMeetingUUID(), existing::setMeetingUUID);
                                setIfPresent(newRace.getRaceId(), existing::setRaceId);
                                setIfPresent(newRace.getStatus(), existing::setStatus);
                                setIfPresent(newRace.getRaceSiteUrl(), existing::setRaceSiteUrl);
                                setIfPresent(newRace.getVenueId(), existing::setVenueId);
                                raceNeedUpdateOrInsert.add(existing);
                            } else {
                                newRace.setMeetingId(meetingUUIDMap.get(newRace.getMeetingUUID()).getId());
                                raceNeedUpdateOrInsert.add(newRace);
                            }
                        });

                        log.info("Race need to be update is {}", raceNeedUpdateOrInsert.size());
                        return raceNeedUpdateOrInsert;
                    })
                    .flatMap(raceNeedUpdateOrInsert -> Flux.fromIterable(raceNeedUpdateOrInsert)
                                                           .flatMap(race -> raceRepository.save(race))
                                                           .collectList()
                    )
                    .flatMap(savedRace -> {
                        savedRace.stream().map(KafkaDtoMapper::convertToKafkaRaceDto).forEach(race -> {
                            KafkaPayload payload = new KafkaPayload.Builder().eventType(EventTypeEnum.GENERIC).actualPayload((new Gson().toJson(race))).build();
                            kafkaService.publishKafka(payload, race.getId().toString(), null);
                        });
                        saveRaceToTodayData(savedRace);

                        List<RaceSite> raceSites = savedRace.stream().map(race -> RaceResponseMapper.toRacesiteDto(race, SiteEnum.LAD_BROKE.getId(), race.getId())).collect(Collectors.toList());
                        Map<String, Long> mapRaceUUIDAndId = savedRace.stream().filter(race -> race.getRaceId() != null && race.getId() != null)
                                .collect(Collectors.toMap(Race::getRaceId, Race::getId, (first, second) -> first));

                        return crawUtils.saveRaceSite(Flux.fromIterable(raceSites), SiteEnum.LAD_BROKE.getId())
                                        .then(Mono.just(mapRaceUUIDAndId));
                    })
                    .flatMapMany(mapRaceUUIDAndId -> crawlAndSaveEntrants(mapRaceUUIDAndId, date))
                    .doOnComplete(() -> log.info("All races and entrants processed successfully"))
                    .thenMany(getMeetingFromAllSite(date))
                    .then();
    }

    private void saveRaceToTodayData(List<Race> savedRace) {
        if (todayData.getRaces() == null) {
            todayData.setRaces(new TreeMap<>());
        }
        // remove yesterday data
        if (! todayData.getRaces().isEmpty()) {
            Long startOfToday = Instant.now().atZone(ZoneOffset.UTC).with(LocalTime.MIN).minusHours(2).toInstant().toEpochMilli();
            todayData.setRaces(new TreeMap<>(todayData.getRaces().tailMap(startOfToday)));
        }

        savedRace.forEach(race -> todayData.addOrUpdateRace(race.getAdvertisedStart().toEpochMilli(), race.getId()));
    }

    private Flux<Entrant> crawlAndSaveEntrants(Map<String, Long> mapRaceUUIDAndId, LocalDate date) {
        AtomicBoolean isApiRequestFailed = new AtomicBoolean(false);

        return Flux.fromIterable(mapRaceUUIDAndId.entrySet())
                   .flatMap(entry -> getEntrantByRaceId(entry.getKey(), entry.getValue()))
                   .onErrorContinue((throwable, o) -> {
                       if (throwable instanceof ApiRequestFailedException) {
                           isApiRequestFailed.set(true);
                       } else {
                           log.error("Got exception \"{}\" while crawling race caused by {}", throwable.getMessage(), o);
                       }
                   })
                   .doFinally(signalType -> {
                       if (isApiRequestFailed.get()) {
                           crawUtils.saveFailedCrawlMeeting(this.getClass().getName(), date);
                       }
                   });
    }

    private Flux<Entrant> getEntrantByRaceId(String raceId, Long generalRaceId) {

        Mono<LadbrokesRaceApiResponse> ladbrokesRaceApiResponseMono = getLadBrokedItRaceDto(raceId);

        return ladbrokesRaceApiResponseMono
                .mapNotNull(LadbrokesRaceApiResponse::getData)
                .flatMapMany(raceRawData -> {
                    if (raceRawData.getRaces() == null || raceRawData.getMeetings() == null) {
                        return Flux.empty();
                    }
                    Map<String, LadbrokesRaceResult> results = raceRawData.getResults();
                    Map<String, Integer> positions = new HashMap<>();
                    String meetingName = raceRawData.getMeetings().get(raceRawData.getRaces().get(raceId).getMeetingId()).get("name").asText();
                    if(meetingName.contains(" ")){
                        meetingName = meetingName.replace(" ","-").toLowerCase();
                    }
                    String distance = raceRawData.getRaces().get(raceId).getAdditionalInfo().get(AppConstant.DISTANCE).asText();
                    String silkUrl = raceRawData.getRaces().get(raceId).getSilkUrl();
                    String fullFormUrl = raceRawData.getRaces().get(raceId).getFullFormUrl();
                    HashMap<String, ArrayList<Float>> allEntrantPrices = raceRawData.getPriceFluctuations();
                    HashMap<String, LadBrokesPriceOdds> allEntrantPricesPlaces = raceRawData.getPricePlaces();

                    if (results != null) {
                        positions = results.keySet().stream().collect(Collectors.toMap(Function.identity(), key -> results.get(key).getPosition()));
                    } else {
                        positions.put(AppConstant.POSITION, 0);
                    }

                    List<EntrantRawData> allEntrant = CommonUtils.getListEntrant(raceRawData, allEntrantPrices, allEntrantPricesPlaces, raceId, positions);

                    String top4Entrants = null;
                    Map<Integer, String> resultDisplay = null;
                    Optional<String> optionalStatus = getStatusFromRaceMarket(raceRawData.getMarkets());
                    if (optionalStatus.isPresent() && optionalStatus.get().equals(AppConstant.STATUS_FINAL)) {
                        top4Entrants = getWinnerEntrants(allEntrant).map(entrant -> String.valueOf(entrant.getNumber()))
                                .collect(Collectors.joining(","));
                        resultDisplay = Collections.singletonMap(SiteEnum.LAD_BROKE.getId(), top4Entrants);
                    }

                    RaceDto raceDto = RaceResponseMapper.toRaceDTO(raceRawData.getRaces().get(raceId), meetingName, top4Entrants, optionalStatus.orElse(null));

                    return raceRepository.updateDistanceAndResultsDisplayAndSilkUrlAndFullFormUrlById(generalRaceId, distance == null ? 0 : Integer.parseInt(distance), CommonUtils.toJsonb(resultDisplay), silkUrl, fullFormUrl)
                            .thenMany(saveEntrant(allEntrant, raceId, generalRaceId, raceDto));
                });
    }

     private Flux<Entrant> saveEntrant(List<EntrantRawData> entrantRawData, String raceUUID, Long raceId, RaceDto raceDto) {
        List<Entrant> newEntrants = entrantRawData.stream().map(m -> MeetingMapper.toEntrantEntity(m, AppConstant.LAD_BROKE_SITE_ID)).collect(Collectors.toList());
        Flux<Entrant> existedEntrants = entrantRepository.findByRaceId(raceId);

        return existedEntrants
                .collectList()
                .map(existed -> {
                    Map<Integer, Entrant> mapNumberToEntrant = existed.stream().collect(Collectors.toMap(Entrant::getNumber, Function.identity()));

                    newEntrants.forEach(newEntrant -> {
                        Integer entrantNumber = newEntrant.getNumber();
                        if (mapNumberToEntrant.containsKey(entrantNumber)) {
                            Entrant existing = mapNumberToEntrant.get(entrantNumber);
                            setIfPresent(newEntrant.getName(), existing::setName);
                            setIfPresent(newEntrant.getScratchedTime(), existing::setScratchedTime);
                            setIfPresent(newEntrant.isScratched(), existing::setScratched);
                            setIfPresent(newEntrant.getRiderOrDriver(), existing::setRiderOrDriver);
                            setIfPresent(newEntrant.getTrainerName(), existing::setTrainerName);
                            setIfPresent(newEntrant.getLast6Starts(), existing::setLast6Starts);
                            setIfPresent(newEntrant.getBestTime(), existing::setBestTime);
                            setIfPresent(newEntrant.getHandicapWeight(), existing::setHandicapWeight);
                            setIfPresent(newEntrant.getEntrantComment(), existing::setEntrantComment);
                            setIfPresent(newEntrant.getBestMileRate(), existing::setBestMileRate);
                        } else {
                            newEntrant.setRaceId(raceId);
                            mapNumberToEntrant.put(entrantNumber, newEntrant);
                        }
                    });

                    Collection<Entrant> entrantNeedUpdateOrInsert = mapNumberToEntrant.values();
                    log.info("Entrant need to be update is {}", entrantNeedUpdateOrInsert.size());
                    return entrantNeedUpdateOrInsert;
                })
                .flatMap(entrantNeedUpdateOrInsert -> entrantRepository.saveAll(entrantNeedUpdateOrInsert).collectList())
                .flatMapMany(savedEntrants -> {
                    savedEntrants.stream().map(KafkaDtoMapper::convertToKafkaEntrantDto).forEach(entrant -> {
                        KafkaPayload payload = new KafkaPayload.Builder().eventType(EventTypeEnum.GENERIC).actualPayload(new Gson().toJson(entrant)).build();
                        kafkaService.publishKafka(payload, entrant.getId().toString(), null);
                    });

                    log.info("{} entrants save into redis and database", savedEntrants.size());
                    RaceResponseDto raceResponseDto = RaceResponseMapper.toRaceResponseDto(savedEntrants, raceUUID, raceId, raceDto);

                    return raceRedisService.hasKey(raceId).flatMap(hasKey -> {
                        if (Boolean.FALSE.equals(hasKey)) {
                            return raceRepository.findById(raceId).flatMap(race -> {
                                raceResponseDto.setVenueId(race.getVenueId());
                                return raceRedisService.saveRace(raceId, raceResponseDto);
                            });
                        } else {
                            return raceRedisService.updateRace(raceId, raceResponseDto);
                        }
                    }).thenMany(Flux.fromIterable(savedEntrants));
                });
    }

    private Mono<LadbrokesRaceApiResponse> getLadBrokedItRaceDto(String raceUUID) throws ApiRequestFailedException {
        String raceQueryURI = AppConstant.LAD_BROKES_IT_RACE_QUERY.replace(AppConstant.ID_PARAM, raceUUID);
        return crawUtils.crawlData(ladbrokesWebClient, raceQueryURI, LadbrokesRaceApiResponse.class, this.getClass().getName(), 5L);
    }

    //after every thing is implement for first time then we call all site. we need to save all common data first
    @SneakyThrows
    private Flux<MeetingDto> getMeetingFromAllSite(LocalDate date) {
        List<ICrawlService> crawlServices = new ArrayList<>();
        for (String site : AppConstant.SITE_LIST) {
            crawlServices.add(serviceLookup.forBean(ICrawlService.class, site));
        }
        return Flux.fromIterable(crawlServices)
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(x -> x.getTodayMeetings(date).onErrorComplete())
                .sequential();
    }
}
