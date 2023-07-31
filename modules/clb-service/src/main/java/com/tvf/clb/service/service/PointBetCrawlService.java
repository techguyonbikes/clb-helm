package com.tvf.clb.service.service;

import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.entity.*;
import com.tvf.clb.base.model.CrawlEntrantData;
import com.tvf.clb.base.model.CrawlRaceData;
import com.tvf.clb.base.model.pointbet.*;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.ConvertBase;
import com.tvf.clb.service.repository.MeetingRepository;
import com.tvf.clb.service.repository.RaceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
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

    @Autowired
    private WebClient pointBetWebClient;

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {
        log.info("Start getting the API from PointBet.");

        String meetingQueryURI = AppConstant.POINT_BET_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
        String className = this.getClass().getName();

        return crawUtils.crawlData(pointBetWebClient, meetingQueryURI, PointBetMeetingRawData[].class, className, 5L)
                        .doOnError(throwable -> crawUtils.saveFailedCrawlMeeting(className, date))
                        .flatMapIterable(meetingRawData -> getAllAusMeeting(Arrays.asList(meetingRawData), date));
    }

    /**
     * This function crawl and get all entrants price
     */
    @Override
    public Mono<CrawlRaceData> getEntrantByRaceUUID(String raceUUID) {

        return crawlPointBetRaceData(raceUUID)
                .onErrorResume(throwable -> Mono.empty())
                .map(raceRawData -> {
                    Map<Integer, Integer> mapEntrantsPositions = new HashMap<>();

                    List<PointBetWinnersRawData> winnersRawData = raceRawData.getResults().getWinners();
                    if (winnersRawData != null) {
                        winnersRawData.forEach(winner -> mapEntrantsPositions.put(Integer.valueOf(winner.getEntrant().getId()), winner.getFinalPlacing()));
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
                });
    }

    private List<MeetingDto> getAllAusMeeting(List<PointBetMeetingRawData> allMeetingRawData, LocalDate date) {
        // get all meeting
        List<MeetingDto> meetingDtoList = allMeetingRawData.stream().map(MeetingMapper::toMeetingDto).collect(Collectors.toList());

        Map<Meeting, List<Race>> mapMeetingAndRace = new HashMap<>();
        for (MeetingDto meetingDto : meetingDtoList) {
            mapMeetingAndRace.put(MeetingMapper.toMeetingEntity(meetingDto), meetingDto.getRaces().stream().map(MeetingMapper::toRaceEntity).collect(Collectors.toList()));
        }
        log.info("Number of meeting: {}", mapMeetingAndRace.keySet().size());
        log.info("Number of race: {}", mapMeetingAndRace.values().stream().mapToLong(List::size).sum());

        List<RaceDto> raceDtoList = meetingDtoList.stream().map(MeetingDto::getRaces).flatMap(List::stream).collect(Collectors.toList());
        Mono<Map<String, Long>> mapUUIDToRaceIdMono = saveMeetingSiteAndRaceSite(mapMeetingAndRace);

        setRaceIdThenCrawlAndSaveEntrants(mapUUIDToRaceIdMono, raceDtoList, date);

        return meetingDtoList;
    }

    private Mono<Map<String, Long>> saveMeetingSiteAndRaceSite(Map<Meeting, List<Race>> mapMeetingAndRace) {
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

            Optional<Integer> firstRaceNumberOptional = newRaces.stream().map(Race::getNumber).min(Comparator.naturalOrder());

            if (firstRaceNumberOptional.isPresent()) {
                int firstRaceNumber = firstRaceNumberOptional.get();

                Instant yesterday = newMeeting.getAdvertisedDate().minus(1, ChronoUnit.DAYS).atZone(ZoneOffset.UTC).with(LocalTime.MIN).toInstant();
                Mono<Long> meetingId = meetingRepository.getMeetingIdsByNameContainsAndRaceTypeAndAdvertisedDateFrom(newMeeting.getName(), newMeeting.getRaceType(), yesterday)
                        .collectList()
                        .flatMap(meetingIds -> raceRepository.getRaceByMeetingIdInAndNumberAndAdvertisedStart(meetingIds, firstRaceNumber, newMeeting.getAdvertisedDate()))
                        .map(Race::getMeetingId)
                        .cache();

                newMeetingSiteFlux = newMeetingSiteFlux.concatWith(meetingId.map(id -> MeetingMapper.toMetingSite(newMeeting, SiteEnum.POINT_BET.getId(), id)));

                Flux<RaceSite> raceSites = crawUtils.getRaceSitesFromMeetingIdAndRaces(meetingId, newRaces, SiteEnum.POINT_BET.getId()).cache();

                newRaceSiteFlux = newRaceSiteFlux.concatWith(raceSites);
            }
        }

        return crawUtils.saveMeetingSite(mapMeetingAndRace.keySet(), newMeetingSiteFlux, SiteEnum.POINT_BET.getId())
                .thenMany( crawUtils.saveRaceSite(newRaceSiteFlux, SiteEnum.POINT_BET.getId()))
                .then(newRaceSiteFlux.collectMap(RaceSite::getRaceSiteId, RaceSite::getGeneralRaceId));
    }


    @Override
    public Flux<EntrantDto> crawlAndSaveEntrantsInRace(RaceDto raceDto, LocalDate date) {

        String raceUUID = raceDto.getId();

        return crawlPointBetRaceData(raceUUID)
                .doOnError(throwable -> crawUtils.saveFailedCrawlRace(this.getClass().getName(), raceDto, date))
                .flatMapMany(raceRawData -> {
                    List<PointBetEntrantRawData> entrants = raceRawData.getEntrants();
                    String statusRace = ConvertBase.getRaceStatusById(raceRawData.getTradingStatus(), raceRawData.getResultStatus());
                    // Map entrant id to prices
                    Map<String, List<Float>> allEntrantPrices = getEntrantsPriceFromRaceRawData(raceRawData);
                    // Convert to entity and save from raw data
                    List<Entrant> listEntrantEntity = EntrantMapper.toListEntrantEntity(entrants, allEntrantPrices, raceUUID);

                    return Mono.justOrEmpty(raceDto.getRaceId())
                            .switchIfEmpty(crawUtils.getIdForNewRaceAndSaveRaceSite(raceDto, listEntrantEntity, SiteEnum.POINT_BET.getId())
                                                    .doOnNext(raceDto::setRaceId)
                            ) // Get id for new race and save race site if raceDto.getRaceId() == null
                            .flatMapIterable(raceId -> {
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

                                raceDto.setDistance(raceRawData.getRaceDistance());

                                crawUtils.saveEntrantCrawlDataToRedis(listEntrantEntity, raceDto, AppConstant.POINT_BET_SITE_ID);
                                crawUtils.saveEntrantsPriceIntoDB(listEntrantEntity, raceDto.getRaceId(), AppConstant.POINT_BET_SITE_ID);

                                return listEntrantEntity.stream().map(EntrantMapper::toEntrantDto).collect(Collectors.toList());
                            });
                });

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
    public Mono<PointBetRaceApiResponse> crawlPointBetRaceData(String raceUUID) {
        String raceQueryURI = AppConstant.POINT_BET_RACE_QUERY.replace(AppConstant.ID_PARAM, raceUUID);
        return crawUtils.crawlData(pointBetWebClient, raceQueryURI, PointBetRaceApiResponse.class, this.getClass().getName(), 5L)
                        .filter(raceRawData -> raceRawData != null && raceRawData.getEntrants() != null);
    }

}
