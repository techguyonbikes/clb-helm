package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.entity.*;
import com.tvf.clb.base.exception.ApiRequestFailedException;
import com.tvf.clb.base.model.CrawlEntrantData;
import com.tvf.clb.base.model.CrawlRaceData;
import com.tvf.clb.base.model.pointbet.*;
import com.tvf.clb.base.utils.ApiUtils;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.ConvertBase;
import com.tvf.clb.service.repository.MeetingRepository;
import com.tvf.clb.service.repository.RaceRepository;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {
        log.info("Start getting the API from PointBet.");

        CrawlMeetingFunction crawlFunction = crawlDate -> {
            String url = AppConstant.POINT_BET_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
            Response response = ApiUtils.get(url);
            ResponseBody body = response.body();
            Gson gson = new GsonBuilder().setDateFormat(AppConstant.DATE_TIME_FORMAT_LONG).create();
            if (body != null) {
                TypeToken<List<PointBetMeetingRawData>> type = new TypeToken<List<PointBetMeetingRawData>>() {};
                List<PointBetMeetingRawData> rawData = gson.fromJson(body.string(), type.getType());

                return getAllAusMeeting(rawData, date);
            }
            return null;
        };

        return crawUtils.crawlMeeting(crawlFunction, date, 20000L, this.getClass().getName());
    }

    /**
     * This function crawl and get all entrants price
     */
    @Override
    public CrawlRaceData getEntrantByRaceUUID(String raceUUID) {

        PointBetRaceApiResponse raceRawData = crawlPointBetRaceData(raceUUID);

        if (raceRawData == null) {
            return new CrawlRaceData();
        }
        Map<Integer, Integer> mapEntrantsPositions = new HashMap<>();
        if (raceRawData.getResults() != null) {
            mapEntrantsPositions = raceRawData.getResults().getWinners() == null ? new HashMap<>() : raceRawData.getResults().getWinners().stream()
                    .collect(Collectors.toMap(result -> Integer.valueOf(result.getEntrant().getId()), PointBetWinnersRawData::getFinalPlacing));
        }

        Map<String, List<Float>> allEntrantPrices = getEntrantsPriceFromRaceRawData(raceRawData);
        Map<Integer, CrawlEntrantData> mapEntrants = new HashMap<>();


        for (PointBetEntrantRawData entrant : raceRawData.getEntrants()) {
            Map<Integer, List<Float>> priceFluctuations = new HashMap<>();
            priceFluctuations.put(AppConstant.POINT_BET_SITE_ID, allEntrantPrices.getOrDefault(entrant.getId(), new ArrayList<>()));
            mapEntrants.put(Integer.valueOf(entrant.getId()), new CrawlEntrantData(mapEntrantsPositions.getOrDefault(Integer.valueOf(entrant.getId()), 0), priceFluctuations));
        }

        CrawlRaceData result = new CrawlRaceData();
        result.setSiteEnum(SiteEnum.POINT_BET);
        result.setMapEntrants(mapEntrants);

        String statusRace = ConvertBase.getRaceStatusById(raceRawData.getTradingStatus(), raceRawData.getResultStatus());
        result.setStatus(statusRace);
        if (AppConstant.STATUS_FINAL.equals(statusRace)) {
            result.setFinalResult(Collections.singletonMap(AppConstant.POINT_BET_SITE_ID, raceRawData.getPlacing()));
        }

        return result;
    }

    private List<MeetingDto> getAllAusMeeting(List<PointBetMeetingRawData> allMeetingRawData, LocalDate date) {
        // get all meeting
        List<MeetingDto> meetingDtoList = allMeetingRawData.stream().map(MeetingMapper::toMeetingDto).collect(Collectors.toList());

        Map<Meeting, List<Race>> mapMeetingAndRace = new HashMap<>();
        for (MeetingDto meetingDto : meetingDtoList) {
            mapMeetingAndRace.put(MeetingMapper.toMeetingEntity(meetingDto), meetingDto.getRaces().stream().map(MeetingMapper::toRaceEntity).collect(Collectors.toList()));
        }
        log.info("number of meeting: {}", mapMeetingAndRace.keySet().size());
        log.info("number of race: {}", mapMeetingAndRace.values().size());

        List<RaceDto> raceDtoList = meetingDtoList.stream().map(MeetingDto::getRaces).flatMap(List::stream).collect(Collectors.toList());
        Mono<Map<String, Long>> mapUUIDToRaceIdMono = saveMeetingSiteAndRaceSite(mapMeetingAndRace);

        setRaceIdThenCrawlAndSaveEntrants(mapUUIDToRaceIdMono, raceDtoList, date);

        return meetingDtoList;
    }

    public Mono<Map<String, Long>> saveMeetingSiteAndRaceSite(Map<Meeting, List<Race>> mapMeetingAndRace) {
        /* Can not use CrawUtils.saveMeetingSte() directly since PointBet save Meeting.advertisedDate as the first race's start time in the meeting.
           To find generalMeetingId, we need to find meetings have same name and race type first. Then find all the first race in those meetings.
           Then we can get the race has same advertised time with PointBet meeting. So we have generalMeetingId need to find from that race.

           Assume that 2 meetings have same name, type in same day can occur
        */

        Flux<MeetingSite> newMeetingSiteFlux = Flux.empty();
        Flux<RaceSite> newRaceSiteFlux = Flux.empty();

        for (Map.Entry<Meeting, List<Race>> entry : mapMeetingAndRace.entrySet()) {
            Meeting newMeeting = entry.getKey();
            List<Race> newRaces = entry.getValue();

            Instant yesterday = newMeeting.getAdvertisedDate().minus(1, ChronoUnit.DAYS).atZone(ZoneOffset.UTC).with(LocalTime.MIN).toInstant();
            Mono<Long> meetingId = meetingRepository.getMeetingIdsByNameAndRaceTypeAndAdvertisedDateFrom(newMeeting.getName(), newMeeting.getRaceType(), yesterday)
                                    .collectList()
                                    .flatMap(meetingIds -> raceRepository.getRaceByMeetingIdInAndNumberAndAdvertisedStart(meetingIds, 1, newMeeting.getAdvertisedDate()))
                                    .map(Race::getMeetingId);

            newMeetingSiteFlux = newMeetingSiteFlux.concatWith(meetingId.map(id -> MeetingMapper.toMetingSite(newMeeting, SiteEnum.POINT_BET.getId(), id)));

            Flux<RaceSite> raceSites = crawUtils.getRaceSitesFromMeetingIdAndRaces(meetingId, newRaces, SiteEnum.POINT_BET.getId());

            newRaceSiteFlux = newRaceSiteFlux.concatWith(raceSites);
        }

        return crawUtils.saveMeetingSite(mapMeetingAndRace.keySet(), newMeetingSiteFlux, SiteEnum.POINT_BET.getId())
                .thenMany( crawUtils.saveRaceSite(newRaceSiteFlux, SiteEnum.POINT_BET.getId()))
                .then(newRaceSiteFlux.collectMap(RaceSite::getRaceSiteId, RaceSite::getGeneralRaceId));
    }


    @Override
    public Flux<EntrantDto> crawlAndSaveEntrantsInRace(RaceDto raceDto, LocalDate date) {

        String raceUUID = raceDto.getId();

        PointBetRaceApiResponse raceRawData = crawlPointBetRaceData(raceUUID);

        if (raceRawData != null) {
            List<PointBetEntrantRawData> entrants = raceRawData.getEntrants();

            String statusRace = ConvertBase.getRaceStatusById(raceRawData.getTradingStatus(), raceRawData.getResultStatus());

            // Map entrant id to prices
            Map<String, List<Float>> allEntrantPrices = getEntrantsPriceFromRaceRawData(raceRawData);

            // Set position for entrants. Currently, only four winners have position when race completed
            if (StringUtils.hasText(raceRawData.getPlacing())) {
                List<String> winnersId = Arrays.asList(raceRawData.getPlacing().split(","));
                entrants.forEach(entrant -> {
                    if (winnersId.contains(entrant.getId())) {
                        entrant.setPosition(winnersId.indexOf(entrant.getId()) + 1);
                    }
                });
                if (AppConstant.STATUS_FINAL.equals(statusRace)) {
                    raceDto.setDistance(raceRawData.getRaceDistance());
                    raceDto.setFinalResult(raceRawData.getPlacing());
                    crawUtils.updateRaceFinalResultIntoDB(raceDto.getRaceId(), raceRawData.getPlacing(), AppConstant.POINT_BET_SITE_ID);
                }
            }

            // Convert to entity and save from raw data
            List<Entrant> listEntrantEntity = EntrantMapper.toListEntrantEntity(entrants, allEntrantPrices, raceUUID);

            raceDto.setDistance(raceRawData.getRaceDistance());
            crawUtils.saveEntrantCrawlDataToRedis(listEntrantEntity, raceDto, AppConstant.POINT_BET_SITE_ID);

            crawUtils.saveEntrantsPriceIntoDB(listEntrantEntity, raceDto.getRaceId(), AppConstant.POINT_BET_SITE_ID);

            return Flux.fromIterable(listEntrantEntity.stream().map(EntrantMapper::toEntrantDto).collect(Collectors.toList()));
        } else {
            crawUtils.saveFailedCrawlRace(this.getClass().getName(), raceDto, date);
            throw new ApiRequestFailedException();
        }
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
                    .filter(price -> WIN_MARKET.equals(price.getMarketTypeCode()))
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

        CrawlRaceFunction crawlFunction = uuid -> {
            String url = AppConstant.POINT_BET_RACE_QUERY.replace(AppConstant.ID_PARAM, raceUUID);
            Response response = ApiUtils.get(url);
            ResponseBody body = response.body();
            if (body != null) {
                return new Gson().fromJson(body.string(), PointBetRaceApiResponse.class);
            }

            return null;
        };

        Object result = crawUtils.crawlRace(crawlFunction, raceUUID, this.getClass().getName());

        if (result == null) {
            return null;
        }
        return (PointBetRaceApiResponse) result;
    }

}
