package com.tvf.clb.service.service;

import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.model.CrawlEntrantData;
import com.tvf.clb.base.model.CrawlRaceData;
import com.tvf.clb.base.model.betright.*;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.CommonUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@ClbService(componentType = AppConstant.BET_RIGHT)
@Slf4j
@AllArgsConstructor
public class BetRightCrawlService implements ICrawlService {

    private final CrawUtils crawUtils;
    private final WebClient betRightWebClient;

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {
        log.info("Start getting the API from BetRight.");

        String meetingQueryURI = AppConstant.BET_RIGHT_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
        String className = this.getClass().getName();

        return crawUtils.crawlData(betRightWebClient, meetingQueryURI, BetRightMeetingRawData.class, className, 5L)
                .doOnError(throwable -> crawUtils.saveFailedCrawlMeeting(className, date))
                .flatMapIterable(betRightMeetingRawData -> getAllAusMeeting(betRightMeetingRawData, date));
    }

    private List<MeetingDto> getAllAusMeeting(BetRightMeetingRawData betRightMeetingRawData, LocalDate date) {
        int sumMeetingCount = betRightMeetingRawData.getGreyMeetingRawData().size() +
                betRightMeetingRawData.getHorseMeetingRawData().size() +
                betRightMeetingRawData.getHarnessMeetingRawData().size();
        int sumRaceCount = betRightMeetingRawData.getGreyMeetingRawData().stream().mapToInt(data -> data.getRaces().size()).sum() +
                betRightMeetingRawData.getHorseMeetingRawData().stream().mapToInt(data -> data.getRaces().size()).sum() +
                betRightMeetingRawData.getHarnessMeetingRawData().stream().mapToInt(data -> data.getRaces().size()).sum();
        log.info("[BetRight] Sum all meetings: {}", sumMeetingCount);
        log.info("[BetRight] Sum all races: {}", sumRaceCount);
        List<MeetingDto> meetingDtoList = new ArrayList<>();
        Map<Meeting, List<Race>> mapMeetingAndRace = new HashMap<>();

        getRacesInMeeting(betRightMeetingRawData.getHorseMeetingRawData(), meetingDtoList, mapMeetingAndRace);
        getRacesInMeeting(betRightMeetingRawData.getGreyMeetingRawData(), meetingDtoList, mapMeetingAndRace);
        getRacesInMeeting(betRightMeetingRawData.getHarnessMeetingRawData(), meetingDtoList, mapMeetingAndRace);

        List<RaceDto> raceDtoList = meetingDtoList.stream().map(MeetingDto::getRaces).flatMap(List::stream).collect(Collectors.toList());
        Mono<Map<String, Long>> mapUUIDToRaceIdMono = crawUtils.saveMeetingSiteAndRaceSite(mapMeetingAndRace, SiteEnum.BET_RIGHT.getId());
        setRaceIdThenCrawlAndSaveEntrants(mapUUIDToRaceIdMono, raceDtoList, date);
        return meetingDtoList;
    }

    private void getRacesInMeeting(List<BetRightMeetingRaceTypeRawData> raceData, List<MeetingDto> meetingDtoList, Map<Meeting, List<Race>> mapMeetingAndRace) {
        raceData.forEach(meeting -> {
            List<BetRightMeetingRacesRawData> races = meeting.getRaces().stream().sorted(Comparator.comparing(BetRightMeetingRacesRawData::getRaceNumber)).collect(Collectors.toList());
            MeetingDto meetingDto = MeetingMapper.toMeetingDtoFromBetRights(meeting, races);
            meetingDtoList.add(meetingDto);
            mapMeetingAndRace.put(MeetingMapper.toMeetingEntity(meetingDto), meetingDto.getRaces().stream().map(MeetingMapper::toRaceEntity).collect(Collectors.toList()));
        });
    }

    @Override
    public Mono<CrawlRaceData> getEntrantByRaceUUID(String raceId) {
        return crawlRaceData(raceId)
                .onErrorResume(throwable -> Mono.empty())
                .map(raceRawData -> {
                    List<BetRightEntrantRawData> entrants = raceRawData.getEntrantRawData();
                    // entrants price
                    Map<Integer, List<Float>> allEntrantWinPrices = getEntrantsWinPriceFromRaceRawData(entrants);
                    Map<Integer, List<Float>> allEntrantPlacePrices = getEntrantsPlacePriceFromRaceRawData(entrants);
                    //entrants deduction and winner
                    Map<Integer, BetRightDeductionsRawData> allEntrantDeductions = new HashMap<>();
                    Map<Integer, BetRightWinnersRawData> allEntrantWinners = new HashMap<>();
                    mapEntrantWinnerAndDeduction(raceRawData.getResults(), allEntrantDeductions, allEntrantWinners);

                    Map<Integer, CrawlEntrantData> mapEntrants = new HashMap<>();

                    for (BetRightEntrantRawData entrant : entrants) {
                        Integer entrantID = entrant.getOutcomeId();

                        Map<Integer, List<Float>> winPriceFluctuations = new HashMap<>();
                        winPriceFluctuations.put(SiteEnum.BET_RIGHT.getId(), allEntrantWinPrices.getOrDefault(entrantID, new ArrayList<>()));

                        Map<Integer, List<Float>> placePriceFluctuations = new HashMap<>();
                        placePriceFluctuations.put(SiteEnum.BET_RIGHT.getId(), allEntrantPlacePrices.getOrDefault(entrantID, new ArrayList<>()));

                        Map<Integer, Float> winDeduction = new HashMap<>();
                        Map<Integer, Float> placeDeduction = new HashMap<>();

                        Integer position = CommonUtils.applyIfNotEmpty(allEntrantWinners.get(entrantID), BetRightWinnersRawData::getFinalPlacing);
                        if (Boolean.TRUE.equals(entrant.getScratched())) {
                            Float deductionWin = CommonUtils.applyIfNotEmpty(allEntrantDeductions.get(entrantID), BetRightDeductionsRawData::getDeductionWin);
                            Float deductionPlace = CommonUtils.applyIfNotEmpty(allEntrantDeductions.get(entrantID), BetRightDeductionsRawData::getDeductionPlace);

                            CommonUtils.applyIfPresent(SiteEnum.BET_RIGHT.getId(), deductionWin, winDeduction::put);
                            CommonUtils.applyIfPresent(SiteEnum.BET_RIGHT.getId(), deductionPlace, placeDeduction::put);
                        }
                        mapEntrants.put(entrantID, new CrawlEntrantData(position != null ? position : 0, winPriceFluctuations, placePriceFluctuations, winDeduction, placeDeduction));
                    }

                    CrawlRaceData result = new CrawlRaceData();
                    result.setSiteEnum(SiteEnum.BET_RIGHT);
                    result.setMapEntrants(mapEntrants);

                    result.setStatus(null);
                    if (AppConstant.BET_RIGHT_FINAL_RESULTS.equals(raceRawData.getResultStatusId())) {
                        String finalResult = raceRawData.getResults().getWinners().stream().map(betRightWinnersRawData ->
                                betRightWinnersRawData.getOutcome().getOutcomeId().toString()).collect(Collectors.joining(","));
                        result.setFinalResult(Collections.singletonMap(SiteEnum.BET_RIGHT.getId(), finalResult));
                    }
                    return result;
                });
    }

    @Override
    public Flux<EntrantDto> crawlAndSaveEntrantsInRace(RaceDto raceDto, LocalDate date) {
        String raceUUID = raceDto.getId();
        return crawlRaceData(raceUUID)
                .doOnError(throwable -> crawUtils.saveFailedCrawlRace(this.getClass().getName(), raceDto, date))
                .filter(result -> result.getEntrantRawData() != null)
                .flatMapMany(raceRawData -> {
                    List<BetRightEntrantRawData> entrants = raceRawData.getEntrantRawData();
                    Integer statusId = raceRawData.getResultStatusId();
                    // entrants price
                    Map<Integer, List<Float>> allEntrantWinPrices = getEntrantsWinPriceFromRaceRawData(entrants);
                    Map<Integer, List<Float>> allEntrantPlacePrices = getEntrantsPlacePriceFromRaceRawData(entrants);
                    //entrants deduction and winner
                    Map<Integer, BetRightDeductionsRawData> allEntrantDeductions = new HashMap<>();
                    Map<Integer, BetRightWinnersRawData> allEntrantWinners = new HashMap<>();
                    mapEntrantWinnerAndDeduction(raceRawData.getResults(), allEntrantDeductions, allEntrantWinners);

                    List<Entrant> listEntrantEntity = EntrantMapper.toListEntrantEntityBetRight(entrants, allEntrantWinPrices, allEntrantPlacePrices, raceUUID,
                            allEntrantDeductions, allEntrantWinners);
                    return Mono.justOrEmpty(raceDto.getRaceId())
                            .switchIfEmpty(crawUtils.getIdForNewRaceAndSaveRaceSite(raceDto, listEntrantEntity, SiteEnum.BET_RIGHT.getId())
                                    .doOnNext(raceDto::setRaceId)
                            ) // Get id for new race and save race site if raceDto.getRaceId() == null
                            .flatMapIterable(raceId -> {
                                //check final status
                                if (AppConstant.BET_RIGHT_FINAL_RESULTS.equals(statusId)) {
                                    String finalResult = raceRawData.getResults().getWinners().stream().map(betRightWinnersRawData ->
                                            betRightWinnersRawData.getOutcome().getOutcomeId().toString()).collect(Collectors.joining(","));
                                    raceDto.setFinalResult(finalResult);
                                    crawUtils.updateRaceFinalResultIntoDB(raceId, finalResult, SiteEnum.BET_RIGHT.getId());
                                }
                                raceDto.setDistance(raceRawData.getRaceDistance());
                                crawUtils.saveEntrantCrawlDataToRedis(listEntrantEntity, raceDto, SiteEnum.BET_RIGHT.getId());
                                crawUtils.saveEntrantsPriceIntoDB(listEntrantEntity, raceDto.getRaceId(), SiteEnum.BET_RIGHT.getId());
                                return listEntrantEntity.stream().map(EntrantMapper::toEntrantDto).collect(Collectors.toList());
                            });
                });
    }

    /**
     * This function extract entrants win price from race raw data
     */
    private Map<Integer, List<Float>> getEntrantsWinPriceFromRaceRawData(List<BetRightEntrantRawData> raceRawData) {
        // Map entrant id to prices
        Map<Integer, List<Float>> allEntrantPrices = new HashMap<>();
        final String WIN_MARKET = "WIN";
        for (BetRightEntrantRawData entrant : raceRawData) {
            entrant.getFixedPrices().stream()
                    .filter(price -> WIN_MARKET.equals(price.getMarketTypeCode()))
                    .findFirst()
                    .ifPresent(price -> {
                        if (Boolean.FALSE.equals(entrant.getScratched())) {
                            List<Float> priceFluctuation = new ArrayList<>();
                            CommonUtils.setIfPresent(price.getOpenPrice(), priceFluctuation::add);
                            CommonUtils.setIfPresent(price.getCurrentFluc2(), priceFluctuation::add);
                            CommonUtils.setIfPresent(price.getCurrentFluc1(), priceFluctuation::add);
                            CommonUtils.setIfPresent(price.getCurrentFluc(), priceFluctuation::add);

                            allEntrantPrices.put(entrant.getOutcomeId(), priceFluctuation);
                        }
                    });
        }
        return allEntrantPrices;
    }

    /**
     * This function extract entrants place price from race raw data
     */
    private Map<Integer, List<Float>> getEntrantsPlacePriceFromRaceRawData(List<BetRightEntrantRawData> raceRawData) {
        // Map entrant id to prices
        Map<Integer, List<Float>> allEntrantPrices = new HashMap<>();
        final String PLACE_MARKET = "PLC";
        for (BetRightEntrantRawData entrant : raceRawData) {
            entrant.getFixedPrices().stream()
                    .filter(price -> PLACE_MARKET.equals(price.getMarketTypeCode()))
                    .findFirst()
                    .ifPresent(price -> {
                        if (price.getPrice() != null && Boolean.FALSE.equals(entrant.getScratched()) && price.getPrice() != 0) {
                            allEntrantPrices.put(entrant.getOutcomeId(), Collections.singletonList(price.getPrice()));
                        }
                    });
        }
        return allEntrantPrices;
    }

    private void mapEntrantWinnerAndDeduction(BetRightResultsRawData results,
                                              Map<Integer, BetRightDeductionsRawData> allEntrantDeductions,
                                              Map<Integer, BetRightWinnersRawData> allEntrantWinners){
        List<BetRightWinnersRawData> winners = results.getWinners();
        List<BetRightDeductionsRawData> deductions = results.getDeductions();
        if (!CollectionUtils.isEmpty(deductions)) {
            deductions.forEach(deductionsRawData -> allEntrantDeductions.put(deductionsRawData.getOutcomeId(), deductionsRawData));
        }
        if (!CollectionUtils.isEmpty(winners)) {
            winners.forEach(winnersRawData -> allEntrantWinners.put(winnersRawData.getOutcome().getOutcomeId(), winnersRawData));
        }
    }

    private Mono<BetRightRaceRawData> crawlRaceData(String raceId) {
        String raceQueryURI = AppConstant.BET_RIGHT_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
        return crawUtils.crawlData(betRightWebClient, raceQueryURI, BetRightRaceRawData.class, this.getClass().getName(), 5L);
    }
}
