package com.tvf.clb.service.service;

import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.model.*;
import com.tvf.clb.base.model.ladbrokes.*;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.CommonUtils;
import com.tvf.clb.base.utils.ConvertBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ClbService(componentType =  AppConstant.NED)
@Slf4j
public class NedsCrawlService implements ICrawlService{

    @Autowired
    private CrawUtils crawUtils;

    @Autowired
    private WebClient nedsWebClient;

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {

        log.info("Start getting the API from Ned.");

        String meetingQueryURI = AppConstant.NEDS_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
        String className = this.getClass().getName();

        return crawUtils.crawlData(nedsWebClient, meetingQueryURI, LadBrokedItMeetingDto.class, className, 5L)
                .doOnError(throwable -> crawUtils.saveFailedCrawlMeeting(className, date))
                .flatMapIterable(nedMeetingRawData -> getAllAusMeeting(nedMeetingRawData, date));
    }

    @Override
    public Mono<CrawlRaceData> getEntrantByRaceUUID(String raceId) {
        Mono<LadbrokesRaceApiResponse> nedsRaceApiResponseMono = getNedsRaceDto(raceId);

        return nedsRaceApiResponseMono
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

                    Map<Integer, CrawlEntrantData> mapEntrants = new HashMap<>();
                    allEntrant.forEach(x -> {
                        Map<Integer, List<Float>> priceFluctuations = new HashMap<>();
                        priceFluctuations.put(AppConstant.NED_SITE_ID, x.getPriceFluctuations());

                        Map<Integer, List<Float>> pricePlaces = new HashMap<>();
                        pricePlaces.put(AppConstant.NED_SITE_ID, x.getPricePlaces());

                        Map<Integer, Float> winDeduction = new HashMap<>();
                        CommonUtils.applyIfPresent(AppConstant.NED_SITE_ID, x.getWinDeduction(), winDeduction::put);

                        Map<Integer, Float> placeDeduction = new HashMap<>();
                        CommonUtils.applyIfPresent(AppConstant.NED_SITE_ID, x.getPlaceDeduction(), placeDeduction::put);

                        mapEntrants.put(x.getNumber(), new CrawlEntrantData(x.getPosition(), priceFluctuations, pricePlaces, winDeduction, placeDeduction));
                    });

                    CrawlRaceData result = new CrawlRaceData();
                    result.setSiteEnum(SiteEnum.NED);
                    result.setMapEntrants(mapEntrants);

                    Optional<String> optionalStatus = getStatusFromRaceMarket(raceDto.getMarkets());
                    if (optionalStatus.isPresent() && optionalStatus.get().equals(AppConstant.STATUS_FINAL)) {
                        String top4Entrants = getWinnerEntrants(allEntrant)
                                .map(entrant -> String.valueOf(entrant.getNumber()))
                                .collect(Collectors.joining(","));

                        result.setFinalResult(Collections.singletonMap(AppConstant.NED_SITE_ID, top4Entrants));
                    }

                    return result;
                });
    }

    private List<MeetingDto> getAllAusMeeting(LadBrokedItMeetingDto ladBrokedItMeetingDto, LocalDate date) {

        Collection<VenueRawData> venues = ladBrokedItMeetingDto.getVenues().values();
        log.info("[NEDS] Sum all meetings: {}", ladBrokedItMeetingDto.getMeetings().values().size());
        log.info("[NEDS] Sum all races: {}", ladBrokedItMeetingDto.getRaces().values().size());
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

        Map<Meeting, List<Race>> mapMeetingAndRace = new HashMap<>();

        for (MeetingRawData localMeeting : meetings) {
            List<RaceRawData> localRace = races.stream().filter(r -> localMeeting.getRaceIds().contains(r.getId())).collect(Collectors.toList());
            MeetingDto meetingDto = MeetingMapper.toMeetingDto(localMeeting, localRace);
            meetingDtoList.add(meetingDto);
            mapMeetingAndRace.put(MeetingMapper.toMeetingEntity(meetingDto), meetingDto.getRaces().stream().map(MeetingMapper::toRaceEntityFromNED).collect(Collectors.toList()));
        }

        List<RaceDto> raceDtoList = meetingDtoList.stream().map(MeetingDto::getRaces).flatMap(List::stream).collect(Collectors.toList());
        Mono<Map<String, Long>> mapUUIDToRaceIdMono = crawUtils.saveMeetingSiteAndRaceSite(mapMeetingAndRace, SiteEnum.NED.getId());

        setRaceIdThenCrawlAndSaveEntrants(mapUUIDToRaceIdMono, raceDtoList, date);

        return meetingDtoList;
    }

    @Override
    public Flux<EntrantDto> crawlAndSaveEntrantsInRace(RaceDto raceDto, LocalDate date) {

        String raceUUID = raceDto.getId();
        Mono<LadbrokesRaceApiResponse> nedsRaceApiResponseMono = getNedsRaceDto(raceUUID);

        return nedsRaceApiResponseMono
                .doOnError(throwable -> crawUtils.saveFailedCrawlRace(this.getClass().getName(), raceDto, date))
                .mapNotNull(LadbrokesRaceApiResponse::getData)
                .filter(raceRawData -> raceRawData.getRaces() != null || raceRawData.getMeetings() != null)
                .flatMapMany(raceRawData -> {
                    Map<String, LadbrokesRaceResult> results = raceRawData.getResults();
                    Map<String, Integer> positions = new HashMap<>();
                    if (results != null) {
                        positions = results.keySet().stream().collect(Collectors.toMap(Function.identity(), key -> results.get(key).getPosition()));
                    } else {
                        positions.put(AppConstant.POSITION, 0);
                    }

                    HashMap<String, ArrayList<Float>> allEntrantPrices = raceRawData.getPriceFluctuations();
                    HashMap<String, LadBrokesPriceOdds> allEntrantPricesPlaces = raceRawData.getPricePlaces();
                    List<EntrantRawData> allEntrant = CommonUtils.getListEntrant(raceRawData, allEntrantPrices, allEntrantPricesPlaces, raceUUID, positions);

                    List<Entrant> newEntrants = allEntrant.stream().distinct().map(MeetingMapper::toEntrantEntity).collect(Collectors.toList());

                    return Mono.justOrEmpty(raceDto.getRaceId())
                               .switchIfEmpty(crawUtils.getIdForNewRaceAndSaveRaceSite(raceDto, newEntrants, SiteEnum.NED.getId())
                                                       .doOnNext(raceDto::setRaceId)
                               ) // Get id for new race and save race site if raceDto.getRaceId() == null
                               .flatMapIterable(raceId -> {
                                   Optional<String> optionalStatus = getStatusFromRaceMarket(raceRawData.getMarkets());
                                   if (optionalStatus.isPresent() && optionalStatus.get().equals(AppConstant.STATUS_FINAL)) {
                                       String top4Entrants = getWinnerEntrants(allEntrant)
                                               .map(entrant -> String.valueOf(entrant.getNumber()))
                                               .collect(Collectors.joining(","));

                                       raceDto.setFinalResult(top4Entrants);
                                       crawUtils.updateRaceFinalResultIntoDB(raceId, top4Entrants, AppConstant.NED_SITE_ID);
                                   }

                                   saveEntrant(newEntrants, raceDto);

                                   return allEntrant.stream().map(entrant ->
                                           EntrantMapper.toEntrantDto(entrant, entrant.getPriceFluctuations(), entrant.getPricePlaces())).collect(Collectors.toList());
                               });
                });
    }

    private Stream<EntrantRawData> getWinnerEntrants(List<EntrantRawData> entrants) {
        return entrants.stream().parallel()
                .filter(entrant -> entrant.getPosition() > 0)
                .sorted(Comparator.comparing(EntrantRawData::getPosition))
                .limit(4);
    }

    private Optional<String> getStatusFromRaceMarket(Map<String, LadbrokesMarketsRawData> marketsRawDataMap) {
        Optional<LadbrokesMarketsRawData> finalFieldMarket = marketsRawDataMap.values().stream().filter(market -> market.getName().equals(AppConstant.MARKETS_NAME)).findFirst();
        return finalFieldMarket.flatMap(market -> ConvertBase.getLadbrokeRaceStatus(market.getMarketStatusId()));
    }

    private void saveEntrant(List<Entrant> newEntrants, RaceDto raceDto) {
        crawUtils.saveEntrantCrawlDataToRedis(newEntrants, raceDto, AppConstant.NED_SITE_ID);
        crawUtils.saveEntrantsPriceIntoDB(newEntrants, raceDto.getRaceId(), AppConstant.NED_SITE_ID);
    }

    private Mono<LadbrokesRaceApiResponse> getNedsRaceDto(String raceUUID) {
        String raceQueryURI = AppConstant.NEDS_RACE_QUERY.replace(AppConstant.ID_PARAM, raceUUID);
        return crawUtils.crawlData(nedsWebClient, raceQueryURI, LadbrokesRaceApiResponse.class, this.getClass().getName(), 5L);
    }
}
