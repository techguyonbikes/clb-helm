package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.entity.TodayData;
import com.tvf.clb.base.exception.ApiRequestFailedException;
import com.tvf.clb.base.model.*;
import com.tvf.clb.base.utils.ApiUtils;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.ConvertBase;
import com.tvf.clb.service.repository.EntrantRepository;
import com.tvf.clb.service.repository.MeetingRepository;
import com.tvf.clb.service.repository.RaceRepository;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tvf.clb.base.utils.CommonUtils.setIfPresent;

@ClbService(componentType = AppConstant.LAD_BROKE)
@Slf4j
public class LadBrokeCrawlService implements ICrawlService {

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private RaceRepository raceRepository;

    @Autowired
    private EntrantRepository entrantRepository;

    @Autowired
    private CrawUtils crawUtils;

    @Autowired
    private ServiceLookup serviceLookup;

    @Autowired
    private RaceRedisService raceRedisService;

    @Autowired
    private ReactiveRedisTemplate<String, Long> raceNameAndIdTemplate;

    @Autowired
    private TodayData todayData;

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {
        CrawlMeetingFunction crawlFunction = crawlDate -> {
            todayData.setLastTimeCrawl(Instant.now());
            String url = AppConstant.LAD_BROKES_IT_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
            Response response = ApiUtils.get(url);
            ResponseBody body = response.body();
            Gson gson = new GsonBuilder().setDateFormat(AppConstant.DATE_TIME_FORMAT_LONG).create();
            if (body != null) {
                LadBrokedItMeetingDto rawData = gson.fromJson(body.string(), LadBrokedItMeetingDto.class);

                return getAllAusMeeting(rawData, date);
            }
            return null;
        };

        return crawUtils.crawlMeeting(crawlFunction, date, 0L, this.getClass().getName());
    }

    @Override
    public CrawlRaceData getEntrantByRaceUUID(String raceId) {
        LadBrokedItRaceDto raceDto = getLadBrokedItRaceDto(raceId);

        if (raceDto == null || raceDto.getRaces() == null) {
            return new CrawlRaceData();
        }

        Map<String, LadbrokesRaceResult> results = raceDto.getResults();
        Map<String, Integer> positions = new HashMap<>();
        if (results != null) {
            positions = results.keySet().stream().collect(Collectors.toMap(Function.identity(), key -> results.get(key).getPosition()));
        } else {
            positions.put(AppConstant.POSITION, 0);
        }
        HashMap<String, ArrayList<Float>> allEntrantPrices = raceDto.getPriceFluctuations();
        List<EntrantRawData> allEntrant = crawUtils.getListEntrant(raceDto, allEntrantPrices, raceId, positions);

        Map<Integer, CrawlEntrantData> entrantMap = new HashMap<>();
        allEntrant.forEach(x -> {
            List<Float> entrantPrice;
            if (allEntrantPrices == null) {
                entrantPrice = new ArrayList<>();
            } else {
                entrantPrice = allEntrantPrices.get(x.getId()) == null ? new ArrayList<>()
                        : new ArrayList<>(allEntrantPrices.get(x.getId()));
            }

            Map<Integer, List<Float>> priceFluctuations = new HashMap<>();
            priceFluctuations.put(AppConstant.LAD_BROKE_SITE_ID, entrantPrice);
            entrantMap.put(x.getNumber(), EntrantMapper.toCrawlEntrantData(x, AppConstant.LAD_BROKE_SITE_ID));
        });

        CrawlRaceData result = new CrawlRaceData();
        result.setSiteEnum(SiteEnum.LAD_BROKE);
        result.setMapEntrants(entrantMap);

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

        List<RaceRawData> races = ladBrokedItMeetingDto.getRaces().values().stream().filter(r -> raceIds.contains(r.getId())).collect(Collectors.toList());
        List<MeetingDto> meetingDtoList = new ArrayList<>();

        for (MeetingRawData localMeeting : meetings) {
            List<RaceRawData> localRace = races.stream().filter(r -> localMeeting.getRaceIds().contains(r.getId())).collect(Collectors.toList());

            crawUtils.checkMeetingWrongAdvertisedStart(localMeeting, localRace);

            MeetingDto meetingDto = MeetingMapper.toMeetingDto(localMeeting, localRace);
            meetingDtoList.add(meetingDto);

        }

        List<RaceDto> raceDtoList = meetingDtoList.stream().map(MeetingDto::getRaces).flatMap(List::stream).filter(x -> x.getNumber() != null).collect(Collectors.toList());
        saveMeetingAndRace(meetings, raceDtoList, date);

        return meetingDtoList;
    }

    private Flux<Entrant> getEntrantByRaceId(String raceId, Long generalRaceId) {

        LadBrokedItRaceDto raceRawData = getLadBrokedItRaceDto(raceId);

        if (raceRawData != null) {
            if (raceRawData.getRaces() == null || raceRawData.getMeetings() == null) {
                return Flux.empty();
            }
            Map<String, LadbrokesRaceResult> results = raceRawData.getResults();
            Map<String, Integer> positions = new HashMap<>();
            String meetingName = raceRawData.getMeetings().get(raceRawData.getRaces().get(raceId).getMeetingId()).getAsJsonObject().get("name").getAsString();
            if(meetingName.contains(" ")){
                meetingName = meetingName.replace(" ","-").toLowerCase();
            }
            String distance = raceRawData.getRaces().get(raceId).getAdditionalInfo().get(AppConstant.DISTANCE).getAsString();
            HashMap<String, ArrayList<Float>> allEntrantPrices = raceRawData.getPriceFluctuations();


            if (results != null) {
                positions = results.keySet().stream().collect(Collectors.toMap(Function.identity(), key -> results.get(key).getPosition()));
            } else {
                positions.put(AppConstant.POSITION, 0);
            }

            List<EntrantRawData> allEntrant = crawUtils.getListEntrant(raceRawData, allEntrantPrices, raceId, positions);

            String top4Entrants = null;

            Optional<String> optionalStatus = getStatusFromRaceMarket(raceRawData.getMarkets());
            if (optionalStatus.isPresent() && optionalStatus.get().equals(AppConstant.STATUS_FINAL)) {
                top4Entrants = getWinnerEntrants(allEntrant).map(entrant -> String.valueOf(entrant.getNumber()))
                        .collect(Collectors.joining(","));

                crawUtils.updateRaceFinalResultIntoDB(generalRaceId, SiteEnum.LAD_BROKE.getId(), top4Entrants);
            }

            RaceDto raceDto = RaceResponseMapper.toRaceDTO(raceRawData.getRaces().get(raceId), meetingName, top4Entrants, optionalStatus.orElse(null));

            return raceRepository.setUpdateRaceDistanceById(generalRaceId, distance == null ? 0 : Integer.parseInt(distance))
                    .thenMany(saveEntrant(allEntrant, raceId, generalRaceId, raceDto));

        } else {
            throw new ApiRequestFailedException();
        }

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
        List<String> meetingNameList = newMeetings.stream().map(Meeting::getName).collect(Collectors.toList());
        List<String> raceTypeList = newMeetings.stream().map(Meeting::getRaceType).collect(Collectors.toList());
        List<Instant> dates = newMeetings.stream().map(Meeting::getAdvertisedDate).collect(Collectors.toList());
        Flux<Meeting> existedMeeting = meetingRepository
                .findAllByNameInAndRaceTypeInAndAdvertisedDateIn(meetingNameList, raceTypeList, dates);
        existedMeeting
                .collectList()
                           .subscribe(existed ->
                                    {
                                        newMeetings.addAll(existed);
                                        List<Meeting> meetingNeedUpdateOrInsert = newMeetings.stream().distinct().map(m ->
                                        {
                                            if (m.getId() == null) {
                                                existed.stream()
                                                        .filter(x -> x.getName().equals(m.getName()) && x.getAdvertisedDate().equals(m.getAdvertisedDate()) && x.getRaceType().equals(m.getRaceType()))
                                                        .findFirst()
                                                        .ifPresent(meeting -> {
                                                            m.setId(meeting.getId());
                                                            meeting.setMeetingId(m.getMeetingId());
                                                        });
                                            }
                                            return m;
                                        }).filter(e -> !existed.contains(e)).collect(Collectors.toList());
                                        log.info("Meeting need to be update or insert " + meetingNeedUpdateOrInsert.size());
                                        Flux.fromIterable(meetingNeedUpdateOrInsert)
                                                .parallel() // parallelize the processing
                                                .runOn(Schedulers.parallel()) // specify the parallel scheduler
                                                .flatMap(meeting -> meetingRepository.save(meeting))
                                                .sequential() // switch back to sequential processing
                                                .collectList()
                                    .subscribe(savedMeetings -> {
                                        if (! CollectionUtils.isEmpty(savedMeetings)) {
                                            crawUtils.saveMeetingSite(savedMeetings, AppConstant.LAD_BROKE_SITE_ID);
                                        }
                                        savedMeetings.addAll(existed);
                                        saveRace(raceDtoList, savedMeetings, date);
                                        log.info("All meetings processed successfully");
                                    });
                        }
                );
    }

    public void saveRace(List<RaceDto> raceDtoList, List<Meeting> savedMeeting, LocalDate date) {
        Map<String, Meeting> meetingUUIDMap = savedMeeting.stream()
                                                    .filter(meeting -> meeting.getMeetingId() != null)
                                                    .collect(Collectors.toMap(Meeting::getMeetingId, Function.identity(), (first, second) -> first));
        List<Race> newRaces = raceDtoList.stream().map(MeetingMapper::toRaceEntity).filter(x -> x.getNumber() != null).collect(Collectors.toList());
        List<Integer> raceNumbers = newRaces.stream().map(Race::getNumber).collect(Collectors.toList());
        List<Long> meetingIds = savedMeeting.stream().map(Meeting::getId).collect(Collectors.toList());
        Flux<Race> existedRaces = raceRepository.findAllByNumberInAndMeetingIdIn(raceNumbers, meetingIds);

        existedRaces.collectList()
                .subscribe(existed ->
                        {

                            Map<String, Race> raceMeetingIdNumberMap = existed.stream()
                                    .collect(Collectors.toMap(race -> race.getMeetingId() + " " + race.getNumber(), Function.identity()));
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
                                } else {
                                    newRace.setMeetingId(meetingUUIDMap.get(newRace.getMeetingUUID()).getId());
                                    raceMeetingIdNumberMap.put(key, newRace);
                                }
                            });
                            List<Race> raceNeedUpdateOrInsert = new ArrayList<>(raceMeetingIdNumberMap.values());

                            log.info("Race need to be update is " + raceNeedUpdateOrInsert.size());
                            Flux.fromIterable(raceNeedUpdateOrInsert)
                                    .parallel() // parallelize the processing
                                    .runOn(Schedulers.parallel()) // specify the parallel scheduler
                                    .flatMap(race -> raceRepository.save(race))
                                    .sequential() // switch back to sequential processing
                                    .collectList()
                                    .subscribe(savedRace -> {
                                        if (! CollectionUtils.isEmpty(savedRace)) {
                                            crawUtils.saveRaceSite(savedRace, AppConstant.LAD_BROKE_SITE_ID);
                                            savedRace.forEach(race -> {
                                                Meeting raceMeeting = meetingUUIDMap.get(race.getMeetingUUID());
                                                String key = String.format("%s - %s - %s - %s", raceMeeting.getName(), race.getNumber(), raceMeeting.getRaceType(), date);
                                                raceNameAndIdTemplate.opsForValue().set(key, race.getId(), Duration.ofDays(1)).subscribe();
                                            });
                                        }

                                        savedRace.addAll(existed);
                                        Map<String, Long> mapRaceUUIDAndId = savedRace.stream().filter(race -> race.getRaceId() != null && race.getId() != null)
                                                        .collect(Collectors.toMap(Race::getRaceId, Race::getId, (first, second) -> first));

                                        AtomicBoolean isApiRequestFailed = new AtomicBoolean(false);
                                        Flux.fromIterable(mapRaceUUIDAndId.entrySet())
                                                .parallel()
                                                .runOn(Schedulers.parallel())
                                                .flatMap(entry -> getEntrantByRaceId(entry.getKey(), entry.getValue()))
                                                .sequential()
                                                .collectList()
                                                .onErrorContinue((throwable, o) -> {
                                                    if (throwable instanceof ApiRequestFailedException) {
                                                        isApiRequestFailed.set(true);
                                                    } else {
                                                        log.error("Got {} exception while crawling race {}", throwable.getMessage(), o);
                                                    }
                                                })
                                                .doFinally(entrants -> {
                                                    if (isApiRequestFailed.get()) {
                                                        crawUtils.saveFailedCrawlMeeting(this.getClass().getName(), date);
                                                    }
                                                })
                                                .flatMapMany(x -> getMeetingFromAllSite(date))
                                                .subscribe();

                                        saveRaceToTodayData(savedRace);

                                        log.info("All races processed successfully");
                                    });
                        }
                );
    }

    private void saveRaceToTodayData(List<Race> savedRace) {
        if (todayData.getRaces() == null) {
            todayData.setRaces(new TreeMap<>());
        }
        // remove yesterday data
        if (! todayData.getRaces().isEmpty()) {
            Timestamp startOfToday = Timestamp.from(Instant.now().atZone(ZoneOffset.UTC).with(LocalTime.MIN).toInstant());
            todayData.setRaces(new TreeMap<>(todayData.getRaces().tailMap(startOfToday.getTime())));
        }

        savedRace.forEach(race -> {
            todayData.addRace(Timestamp.from(race.getAdvertisedStart()).getTime(), race.getId());
        });
    }

    public Flux<Entrant> saveEntrant(List<EntrantRawData> entrantRawData, String raceUUID, Long raceId, RaceDto raceDto) {
        List<Entrant> newEntrants = entrantRawData.stream().map(m -> MeetingMapper.toEntrantEntity(m, AppConstant.LAD_BROKE_SITE_ID)).collect(Collectors.toList());
        Flux<Entrant> existedEntrants = entrantRepository.findByRaceId(raceId);

        return existedEntrants
                .collectList()
                .flatMapMany(existed ->
                        {
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
                                } else {
                                    newEntrant.setRaceId(raceId);
                                    mapNumberToEntrant.put(entrantNumber, newEntrant);
                                }
                            });

                            Collection<Entrant> entrantNeedUpdateOrInsert = mapNumberToEntrant.values();
                            log.info("Entrant need to be update is {}", entrantNeedUpdateOrInsert.size());

                            return entrantRepository.saveAll(entrantNeedUpdateOrInsert)
                                    .collectList()
                                    .flatMapMany(saved -> {
                                        log.info("{} entrants save into redis and database", saved.size());
                                        RaceResponseDto raceResponseDto = RaceResponseMapper.toRaceResponseDto(saved, raceUUID, raceId, raceDto);

                                        return raceRedisService.hasKey(raceId).flatMap(hasKey -> {
                                            if (Boolean.FALSE.equals(hasKey)) {
                                                return raceRedisService.saveRace(raceId, raceResponseDto);
                                            } else {
                                                return raceRedisService.updateRace(raceId, raceResponseDto);
                                            }
                                        }).thenMany(Flux.fromIterable(saved));
                                    });
                        }
                );
    }

    private LadBrokedItRaceDto getLadBrokedItRaceDto(String raceId) {

        CrawlRaceFunction crawlFunction = uuid -> {
            String url = AppConstant.LAD_BROKES_IT_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
            Response response = ApiUtils.get(url);
            JsonObject jsonObject = JsonParser.parseString(response.body().string()).getAsJsonObject();
            Gson gson = new GsonBuilder().create();
            return gson.fromJson(jsonObject.get("data"), LadBrokedItRaceDto.class);
        };

        Object result = crawUtils.crawlRace(crawlFunction, raceId, this.getClass().getName());

        if (result == null) {
            return null;
        }
        return (LadBrokedItRaceDto) result;
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
                .flatMap(x -> {
                    try {
                        return x.getTodayMeetings(date);
                    } catch (Exception e) {
                        log.error(e.getMessage());
                        return Flux.empty();
                    }
                })
                .sequential();
    }
}
