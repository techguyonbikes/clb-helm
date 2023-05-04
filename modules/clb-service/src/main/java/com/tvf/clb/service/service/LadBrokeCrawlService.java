package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tvf.clb.base.LadbrokesDividendStatus;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        if (raceDto == null) {
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
            entrantMap.put(x.getNumber(), new CrawlEntrantData(x.getPosition(), priceFluctuations));
        });

        CrawlRaceData result = new CrawlRaceData();
        result.setSiteId(SiteEnum.LAD_BROKE.getId());
        result.setMapEntrants(entrantMap);

        if (isRaceCompleted(results, raceDto.getRaces().get(raceId).getDividends())) {
            String top4Entrants = getWinnerEntrants(allEntrant).map(entrant -> String.valueOf(entrant.getNumber()))
                                                               .collect(Collectors.joining(","));
            result.setFinalResult(Collections.singletonMap(AppConstant.LAD_BROKE_SITE_ID, top4Entrants));
        }

        return result;
    }

    private List<MeetingDto> getAllAusMeeting(LadBrokedItMeetingDto ladBrokedItMeetingDto, LocalDate date) {
        List<VenueRawData> ausVenues = ladBrokedItMeetingDto.getVenues().values().stream().filter(v -> AppConstant.VALID_COUNTRY_CODE.contains(v.getCountry())).collect(Collectors.toList());
        List<String> venuesId = ausVenues.stream().map(VenueRawData::getId).collect(Collectors.toList());

        Map<String, String> meetingState = ausVenues.stream().collect(Collectors.toMap(VenueRawData::getId, VenueRawData::getState));

        List<MeetingRawData> meetings = new ArrayList<>(ladBrokedItMeetingDto.getMeetings().values());
        List<MeetingRawData> ausMeetings = meetings.stream().filter(m -> venuesId.contains(m.getVenueId()) && m.getTrackCondition() != null)
                .peek(x -> x.setState(meetingState.get(x.getVenueId()))).collect(Collectors.toList());
        List<String> raceIds = ausMeetings.stream().map(MeetingRawData::getRaceIds).flatMap(List::stream)
                .collect(Collectors.toList());
        List<RaceRawData> ausRace = ladBrokedItMeetingDto.getRaces()
                .values().stream().filter(r -> raceIds.contains(r.getId())).collect(Collectors.toList());
        List<MeetingDto> meetingDtoList = new ArrayList<>();
        for (MeetingRawData localMeeting : ausMeetings) {
            List<RaceRawData> localRace = ausRace.stream().filter(r -> localMeeting.getRaceIds().contains(r.getId())).collect(Collectors.toList());
            MeetingDto meetingDto = MeetingMapper.toMeetingDto(localMeeting, localRace);
            meetingDtoList.add(meetingDto);
        }

        List<RaceDto> raceDtoList = meetingDtoList.stream().map(MeetingDto::getRaces).flatMap(List::stream).filter(x -> x.getNumber() != null).collect(Collectors.toList());
        saveMeetingAndRace(ausMeetings, raceDtoList, date);
        return meetingDtoList;
    }


    private Flux<Entrant> getEntrantByRaceId(String raceId, Long generalRaceId) {

        LadBrokedItRaceDto raceDto = getLadBrokedItRaceDto(raceId);

        if (raceDto != null) {
            Map<String, LadbrokesRaceResult> results = raceDto.getResults();
            Map<String, Integer> positions = new HashMap<>();

            String distance = raceDto.getRaces().get(raceId).getAdditionalInfo().get(AppConstant.DISTANCE).getAsString();
            HashMap<String, ArrayList<Float>> allEntrantPrices = raceDto.getPriceFluctuations();

            if (results != null) {
                positions = results.keySet().stream().collect(Collectors.toMap(Function.identity(), key -> results.get(key).getPosition()));
            } else {
                positions.put(AppConstant.POSITION, 0);
            }

            List<EntrantRawData> allEntrant = crawUtils.getListEntrant(raceDto, allEntrantPrices, raceId, positions);

            String top4Entrants = null;
            if (isRaceCompleted(results, raceDto.getRaces().get(raceId).getDividends())) {
                top4Entrants = getWinnerEntrants(allEntrant).map(entrant -> String.valueOf(entrant.getNumber()))
                                                                   .collect(Collectors.joining(","));

                crawUtils.updateRaceFinalResultIntoDB(generalRaceId, SiteEnum.LAD_BROKE.getId(), top4Entrants);
            }

            return raceRepository.setUpdateRaceDistanceById(generalRaceId, distance == null ? 0 : Integer.parseInt(distance))
                    .thenMany(saveEntrant(allEntrant, raceId, generalRaceId, raceDto, top4Entrants));

        } else {
            throw new ApiRequestFailedException();
        }

    }

    private boolean isRaceCompleted(Map<String, LadbrokesRaceResult> results, List<LadbrokesRaceDividend> dividends) {

        if (results == null || CollectionUtils.isEmpty(dividends)) {
            return false;
        }

        List<LadbrokesDividendStatus> dividendsStatus = dividends.stream().map(LadbrokesRaceDividend::getStatus).collect(Collectors.toList());

        return results.values().stream()
                .map(LadbrokesRaceResult::getResultStatusId)
                .allMatch(statusId -> dividendsStatus.stream().anyMatch(status -> status.getId().equals(statusId)
                                && (status.getName().equalsIgnoreCase(AppConstant.STATUS_FINAL) || status.getName().equalsIgnoreCase(AppConstant.STATUS_INTERIM))));
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
        List<String> raceNames = newRaces.stream().map(Race::getName).collect(Collectors.toList());
        List<Integer> raceNumbers = newRaces.stream().map(Race::getNumber).collect(Collectors.toList());
        List<Long> meetingIds = savedMeeting.stream().map(Meeting::getId).collect(Collectors.toList());
        Flux<Race> existedRaces = raceRepository.findAllByNameInAndNumberInAndMeetingIdIn(raceNames, raceNumbers, meetingIds);

        existedRaces.collectList()
                .subscribe(existed ->
                        {
                            newRaces.addAll(existed);
                            List<Race> raceNeedUpdateOrInsert = newRaces.stream().distinct().map(e ->
                            {
                                Meeting meeting = meetingUUIDMap.getOrDefault(e.getMeetingUUID(), null);
                                if (meeting != null) {
                                    e.setMeetingId(meeting.getId());
                                }
                                if (e.getId() == null) {
                                    existed.stream()
                                            .filter(x -> x.getName().equals(e.getName())
                                                    && x.getNumber().equals(e.getNumber()))
                                            .findFirst()
                                            .ifPresent(race -> {
                                                e.setId(race.getId());
                                                race.setRaceId(e.getRaceId());
                                                race.setMeetingUUID(e.getMeetingUUID());
                                            });
                                }
                                return e;
                            }).filter(e -> !existed.contains(e)).collect(Collectors.toList());
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

                                        Flux.fromIterable(mapRaceUUIDAndId.entrySet())
                                                .parallel()
                                                .runOn(Schedulers.parallel())
                                                .flatMap(entry -> getEntrantByRaceId(entry.getKey(), entry.getValue()))
                                                .sequential()
                                                .collectList()
                                                .doOnError(error -> crawUtils.saveFailedCrawlMeeting(this.getClass().getName(), date))
                                                .onErrorComplete() // complete the pipeline if errors occur
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

    public Flux<Entrant> saveEntrant(List<EntrantRawData> entrantRawData, String raceUUID, Long raceId, LadBrokedItRaceDto raceDto, String finalResult) {
        List<Entrant> newEntrants = entrantRawData.stream().map(m -> MeetingMapper.toEntrantEntity(m, AppConstant.LAD_BROKE_SITE_ID)).collect(Collectors.toList());
        Flux<Entrant> existedEntrants = entrantRepository.findByRaceId(raceId);
        return existedEntrants
                .collectList()
                .flatMapMany(existed ->
                        {
                            newEntrants.addAll(existed);
                            List<Entrant> entrantNeedUpdateOrInsert = newEntrants.stream().distinct().map(e ->
                            {
                                e.setRaceId(raceId);
                                if (e.getId() == null) {
                                    existed.stream()
                                            .filter(x -> x.getName().equals(e.getName())
                                                    && x.getNumber().equals(e.getNumber())
                                                    && x.getBarrier().equals(e.getBarrier()))
                                            .findFirst()
                                            .ifPresent(entrant -> e.setId(entrant.getId()));
                                }
                                return e;
                            }).filter(e -> !existed.contains(e)).collect(Collectors.toList());
                            log.info("Entrant need to be update is {}", entrantNeedUpdateOrInsert.size());
                            return entrantRepository.saveAll(entrantNeedUpdateOrInsert)
                                    .collectList()
                                    .flatMapMany(saved -> {
                                        log.info("{} entrants save into redis and database", saved.size());
                                        return raceRedisService.hasKey(raceId).flatMap(hasKey -> {
                                            if (Boolean.FALSE.equals(hasKey)) {
                                                return raceRedisService.saveRace(raceId, RaceResponseMapper.toRaceResponseDto(saved, raceUUID, raceId, raceDto, finalResult));
                                            } else {
                                                return raceRedisService.updateRaceAdvertisedStart(raceId, raceDto.getRaces().get(raceUUID).getAdvertisedStart());
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
