package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.MeetingSite;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.exception.ApiRequestFailedException;
import com.tvf.clb.base.model.CrawlEntrantData;
import com.tvf.clb.base.model.pointbet.PointBetEntrantRawData;
import com.tvf.clb.base.model.pointbet.PointBetMeetingRawData;
import com.tvf.clb.base.model.pointbet.PointBetPriceFluctuation;
import com.tvf.clb.base.model.pointbet.PointBetRaceApiResponse;
import com.tvf.clb.base.utils.ApiUtils;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.service.repository.MeetingRepository;
import com.tvf.clb.service.repository.MeetingSiteRepository;
import com.tvf.clb.service.repository.RaceRepository;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@ClbService(componentType = AppConstant.POINT_BET)
@Slf4j
public class PointBetCrawlService implements ICrawlService {

    @Autowired
    private CrawUtils crawUtils;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private RaceRepository raceRepository;

    @Autowired
    private MeetingSiteRepository meetingSiteRepository;

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {
        return Mono.fromSupplier(() -> {
            List<PointBetMeetingRawData> rawData = null;
            try {
                Thread.sleep(20000);
                String url = AppConstant.POINT_BET_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
                Response response = ApiUtils.get(url);
                ResponseBody body = response.body();
                Gson gson = new GsonBuilder().setDateFormat(AppConstant.DATE_TIME_FORMAT_LONG).create();
                if (body != null) {
                    TypeToken<List<PointBetMeetingRawData>> type = new TypeToken<List<PointBetMeetingRawData>>(){};
                    rawData = gson.fromJson(body.string(), type.getType());
                }
            } catch (IOException e) {
                throw new ApiRequestFailedException("POINT BET Meeting API request failed: " + e.getMessage(), e);
            } catch (InterruptedException e) {
                log.warn("Interrupted for 20 seconds!");
                Thread.currentThread().interrupt();
            }
            log.info("Start getting the API from Point Bet.");
            return getAllAusMeeting(rawData, date);
        }).flatMapMany(Flux::fromIterable);
    }

    /**
     * This function crawl and get all entrants price
     */
    @Override
    public Map<Integer, CrawlEntrantData> getEntrantByRaceUUID(String raceUUID) {
        PointBetRaceApiResponse raceRawData = crawlPointBetRaceData(raceUUID);
        List<PointBetEntrantRawData> entrants = raceRawData.getEntrants();
        Map<String, List<Float>> allEntrantPrices = getEntrantsPriceFromRaceRawData(raceRawData);

        Map<Integer, CrawlEntrantData> result = new HashMap<>();

        if (StringUtils.hasText(raceRawData.placing)) {
            List<String> winnersId = Arrays.asList(raceRawData.getPlacing().split(","));
            entrants.forEach(entrant -> {
                if (winnersId.contains(entrant.getId())) {
                    entrant.setPosition(winnersId.indexOf(entrant.getId()) + 1);
                }
            });
        }

        raceRawData.getEntrants().forEach(entrant -> {
            Map<Integer, List<Float>> priceFluctuations = new HashMap<>();
            priceFluctuations.put(AppConstant.POINT_BET_SITE_ID, allEntrantPrices.getOrDefault(entrant.getId(), new ArrayList<>()));
            result.put(Integer.valueOf(entrant.getId()), new CrawlEntrantData(entrant.getPosition(), AppConstant.POINT_BET_SITE_ID, priceFluctuations));
        });

        return result;
    }

    private List<MeetingDto> getAllAusMeeting(List<PointBetMeetingRawData> allMeetingRawData, LocalDate date) {
        // get all australia meeting
        List<PointBetMeetingRawData> ausMeetings = allMeetingRawData.stream()
                .filter(meeting -> AppConstant.VALID_COUNTRY_CODE.contains(meeting.getCountryCode()))
                .collect(Collectors.toList());

        List<MeetingDto> meetingDtoList = ausMeetings.stream().map(MeetingMapper::toMeetingDto).collect(Collectors.toList());

        saveMeeting(meetingDtoList);

        List<RaceDto> raceDtoList = meetingDtoList.stream().map(MeetingDto::getRaces).flatMap(List::stream).collect(Collectors.toList());
        saveRace(raceDtoList);

        crawlAndSaveAllEntrants(raceDtoList, date).subscribe();

        return meetingDtoList;
    }

    public void saveMeeting(List<MeetingDto> meetingDtoList) {
        List<Meeting> newMeetings = meetingDtoList.stream().map(MeetingMapper::toMeetingEntity).collect(Collectors.toList());
        saveMeetingSite(newMeetings, AppConstant.POINT_BET_SITE_ID);
    }

    public void saveMeetingSite(List<Meeting> meetings, Integer siteId) {
        /* Can not use CrawUtils.saveMeetingSte() directly since PointBet save Meeting.advertisedDate as the first race's start time in the meeting.
           To find generalMeetingId, we need to find meetings have same name and race type first. Then find all the first race in those meetings.
           Then we can get the race has same advertised time with PointBet meeting. So we have generalMeetingId need to find from that race.

           Assume that 2 meetings have same name, type in same day can occur
        */
        Flux<MeetingSite> newMeetingSite =
                Flux.fromIterable(meetings)
                        .flatMap(meeting -> {
                            Instant yesterday = meeting.getAdvertisedDate().minus(1, ChronoUnit.DAYS).atZone(ZoneOffset.UTC).with(LocalTime.MIN).toInstant();

                            return meetingRepository.getMeetingIdsByNameAndRaceTypeAndAdvertisedDateFrom(meeting.getName(), meeting.getRaceType(), yesterday)
                                    .collectList()
                                    .flatMap(meetingIds -> raceRepository.getRaceByMeetingIdInAndNumberAndAdvertisedStart(meetingIds, 1, meeting.getAdvertisedDate()))
                                    .map(race -> MeetingMapper.toMetingSite(meeting, siteId, race.getMeetingId()));
                        });

        Flux<MeetingSite> existedMeetingSite = meetingSiteRepository
                .findAllByMeetingSiteIdInAndSiteId(meetings.stream().map(Meeting::getMeetingId).collect(Collectors.toList()), siteId);

        Flux.zip(newMeetingSite.collectList(), existedMeetingSite.collectList())
                .flatMap(tuple2 -> {
                    tuple2.getT2().forEach(dup -> tuple2.getT1().remove(dup));
                    log.info("Meeting site " + siteId + " need to be update is " + tuple2.getT1().size());
                    return meetingSiteRepository.saveAll(tuple2.getT1());
                }).subscribe();

    }

    public void saveRace(List<RaceDto> raceDtoList) {
        List<Race> newRaces = raceDtoList.stream().map(MeetingMapper::toRaceEntity).collect(Collectors.toList());
        crawUtils.saveRaceSite(newRaces, AppConstant.POINT_BET_SITE_ID);
    }

    private Flux<EntrantDto> crawlAndSaveAllEntrants(List<RaceDto> raceDtoList, LocalDate date) {
        return Flux.fromIterable(raceDtoList)
                .parallel() // create a parallel flux
                .runOn(Schedulers.parallel()) // specify which scheduler to use for the parallel execution
                .flatMap(raceDto -> crawlAndSaveEntrantsInRace(raceDto, date)) // call the getRaceById method for each raceId
                .sequential(); // convert back to a sequential flux
    }

    public Flux<EntrantDto> crawlAndSaveEntrantsInRace(RaceDto raceDto, LocalDate date) {

        String raceUUID = raceDto.getId();

        PointBetRaceApiResponse raceRawData = crawlPointBetRaceData(raceUUID);
        List<PointBetEntrantRawData> entrants = raceRawData.getEntrants();

        // Map entrant id to prices
        Map<String, List<Float>> allEntrantPrices = getEntrantsPriceFromRaceRawData(raceRawData);

        // Set position for entrants. Currently, only four winners have position when race completed
        if (StringUtils.hasText(raceRawData.placing)) {
            List<String> winnersId = Arrays.asList(raceRawData.getPlacing().split(","));
            entrants.forEach(entrant -> {
                if (winnersId.contains(entrant.getId())) {
                    entrant.setPosition(winnersId.indexOf(entrant.getId()) + 1);
                }
            });
        }

        // Convert to entity and save from raw data
        List<Entrant> listEntrantEntity = EntrantMapper.toListEntrantEntity(entrants, allEntrantPrices, raceUUID);

        String raceIdIdentifier = String.format("%s - %s - %s - %s", raceDto.getMeetingName(), raceDto.getNumber(), raceDto.getRaceType(), date);
        crawUtils.saveEntrantIntoRedis(listEntrantEntity, AppConstant.POINT_BET_SITE_ID, raceIdIdentifier, raceUUID);

        crawUtils.saveEntrantsPriceIntoDB(listEntrantEntity, raceDto, AppConstant.POINT_BET_SITE_ID);

        return Flux.fromIterable(listEntrantEntity.stream().map(EntrantMapper::toEntrantDto).collect(Collectors.toList()));
    }

    /**
     * This function extract entrants price from race raw data
     */
    private Map<String, List<Float>> getEntrantsPriceFromRaceRawData(PointBetRaceApiResponse raceRawData) {

        // Map entrant id to prices
        Map<String, List<Float>> allEntrantPrices = new HashMap<>();
        final String WIN_MARKET = "WIN";

        for (PointBetEntrantRawData entrant : raceRawData.getEntrants()) {

            entrant.getPrices().stream()
                    .filter(price -> price.getMarketTypeCode().equals(WIN_MARKET))
                    .findFirst()
                    .ifPresent(winPrice -> {
                        if (! CollectionUtils.isEmpty(winPrice.getFlucs())) {
                            List<Float> priceFluctuation = winPrice.getFlucs().stream().map(PointBetPriceFluctuation::getPrice).collect(Collectors.toList());
                            allEntrantPrices.put(entrant.getId(), priceFluctuation);
                        }
                    });
        }

        return allEntrantPrices;
    }

    /**
     * This function crawl Race data from POINT BET
     */
    public PointBetRaceApiResponse crawlPointBetRaceData(String raceUUID) {
        String url = AppConstant.POINT_BET_RACE_QUERY.replace(AppConstant.ID_PARAM, raceUUID);

        try {
            Response response = ApiUtils.get(url);
            ResponseBody body = response.body();
            if (body != null) {
                return new Gson().fromJson(body.string(), PointBetRaceApiResponse.class);
            }
            return null;
        } catch (IOException e) {
            throw new ApiRequestFailedException("POINT BET Race API request failed: " + e.getMessage());
        }

    }

}
