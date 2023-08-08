package com.tvf.clb.service.service;

import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.dto.sportbet.SportBetDataDto;
import com.tvf.clb.base.dto.sportbet.SportBetMeetingDto;
import com.tvf.clb.base.dto.sportbet.SportBetRaceDto;
import com.tvf.clb.base.dto.sportbet.SportBetSectionsDto;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.model.CrawlEntrantData;
import com.tvf.clb.base.model.CrawlRaceData;
import com.tvf.clb.base.model.sportbet.*;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tvf.clb.base.utils.AppConstant.SPORT_BET_BETTING_STATUS_RESULTED;

@ClbService(componentType = AppConstant.SPORT_BET)
@Slf4j
public class SportBetCrawlService implements ICrawlService {

    @Autowired
    private CrawUtils crawUtils;

    @Autowired
    private WebClient sportBetWebClient;

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {
        log.info("Start getting the API from SportBet.");

        String meetingQueryURI = AppConstant.SPORT_BET_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
        String className = this.getClass().getName();

        return crawUtils.crawlData(sportBetWebClient, meetingQueryURI, SportBetDataDto.class, className, 5L)
                .doOnError(throwable -> crawUtils.saveFailedCrawlMeeting(className, date))
                .flatMapIterable(sportBetDataDto -> getAllAusMeeting(sportBetDataDto, date));
    }

    private List<MeetingDto> getAllAusMeeting(SportBetDataDto sportBetDataDto, LocalDate date) {
        List<SportBetSectionsDto> sportBetSectionsDtos = sportBetDataDto.getDates();
        List<SportBetMeetingDto> sportBetMeetingDtoList = sportBetSectionsDtos.get(0).getSections();
        log.info("[SportBet] Sum all meetings: {}", sportBetMeetingDtoList.stream().mapToInt(meeting -> meeting.getMeetings().size()).sum());
        log.info("[SportBet] Sum all meetings: {}", sportBetMeetingDtoList.stream().mapToInt(meeting -> meeting.getMeetings().stream().mapToInt(race -> race.getEvents().size()).sum()).sum());
        List<MeetingDto> meetingDtoList = new ArrayList<>();
        for(SportBetMeetingDto meetingDto :sportBetMeetingDtoList){
            meetingDtoList.addAll(MeetingMapper.toMeetingSportDtoList(meetingDto,meetingDto.getMeetings(),date));
        }
        List<RaceDto> raceDtoList = new ArrayList<>();
        Map<Meeting, List<Race>> mapMeetingAndRace = new HashMap<>();
        meetingDtoList.forEach(meeting -> {
            List<RaceDto> meetingRaces = meeting.getRaces();
            meetingRaces.forEach(race -> {
                race.setMeetingName(meeting.getName());
                race.setRaceType(meeting.getRaceType());
            });
            raceDtoList.addAll(meetingRaces);
            mapMeetingAndRace.put(MeetingMapper.toMeetingEntity(meeting), meetingRaces.stream().map(MeetingMapper::toRaceEntity).collect(Collectors.toList()));
        });

        Mono<Map<String, Long>> mapUUIDToRaceIdMono = crawUtils.saveMeetingSiteAndRaceSite(mapMeetingAndRace, SiteEnum.SPORT_BET.getId());
        setRaceIdThenCrawlAndSaveEntrants(mapUUIDToRaceIdMono, raceDtoList, date);

        return meetingDtoList;
    }

    @Override
    public Mono<CrawlRaceData> getEntrantByRaceUUID(String raceId) {
        return crawlEntrantDataSportBet(raceId)
                .onErrorResume(throwable -> Mono.empty())
                .map(sportBetRaceDto -> {
                    MarketRawData markets = sportBetRaceDto.getMarkets().get(0);
                    List<SportBetEntrantRawData> allEntrant = markets.getSelections();

                    Map<Integer, CrawlEntrantData> entrantMap = new HashMap<>();

                    Map<Integer, SportBetDeduction> mapDeductions = getEntrantDeductions(sportBetRaceDto.getDeductions());

                    allEntrant.forEach(entrant -> {

                        List<Float> winPrices = getPricesFromEntrantStatistics(entrant.getStatistics());
                        Map<Integer, List<Float>> winPriceFluctuations = new HashMap<>();

                        List<Float> placePrices = new ArrayList<>();
                        Map<Integer, List<Float>> placePriceFluctuations = new HashMap<>();

                        entrant.getPrices().stream().filter(r->AppConstant.PRICE_CODE.equals(r.getPriceCode()))
                                .findFirst()
                                .ifPresent(price -> {
                                    winPrices.add(price.getWinPrice());
                                    CommonUtils.setIfPresent(price.getPlacePrice(), placePrices::add);
                                });
                        winPriceFluctuations.put(AppConstant.SPORTBET_SITE_ID, winPrices);
                        placePriceFluctuations.put(AppConstant.SPORTBET_SITE_ID, placePrices);

                        Map<Integer, Float> winDeduction = new HashMap<>();
                        Map<Integer, Float> placeDeduction = new HashMap<>();
                        if (mapDeductions.containsKey(entrant.getRunnerNumber())) {
                            SportBetDeduction deductions = mapDeductions.get(entrant.getRunnerNumber());
                            CommonUtils.applyIfPresent(AppConstant.SPORTBET_SITE_ID, deductions.getWinDeduction(), winDeduction::put);
                            CommonUtils.applyIfPresent(AppConstant.SPORTBET_SITE_ID, deductions.getPlaceDeduction(), placeDeduction::put);
                        }

                        entrantMap.put(entrant.getRunnerNumber(), new CrawlEntrantData(0, winPriceFluctuations, placePriceFluctuations, winDeduction, placeDeduction));
                    });

                    CrawlRaceData result = new CrawlRaceData();
                    result.setSiteEnum(SiteEnum.SPORT_BET);
                    result.setMapEntrants(entrantMap);

                    if (isRaceStatusFinal(sportBetRaceDto)) {
                        String top4Entrants = getWinnerEntrants(sportBetRaceDto.getResults())
                                .limit(4)
                                .map(resultsRawData -> resultsRawData.getRunnerNumber().toString())
                                .collect(Collectors.joining(","));

                        result.setFinalResult(Collections.singletonMap(AppConstant.SPORTBET_SITE_ID, top4Entrants));
                    }

                    return result;
                });
    }

    @Override
    public Flux<EntrantDto> crawlAndSaveEntrantsInRace(RaceDto raceDto, LocalDate date) {
        String raceUUID = raceDto.getId();

        return crawlEntrantDataSportBet(raceUUID)
                .doOnError(throwable -> crawUtils.saveFailedCrawlRace(this.getClass().getName(), raceDto, date))
                .filter(sportBetRaceDto -> ! CollectionUtils.isEmpty(sportBetRaceDto.getMarkets()))
                .flatMapMany(sportBetRaceDto -> {
                    List<Entrant> newEntrants = new ArrayList<>();

                    MarketRawData markets = sportBetRaceDto.getMarkets().get(0);
                    List<SportBetEntrantRawData> allEntrant = markets.getSelections();
                    Map<Integer, SportBetDeduction> mapDeductions = getEntrantDeductions(sportBetRaceDto.getDeductions());

                    for(SportBetEntrantRawData rawData : allEntrant){
                        List<Float> winPrices = getPricesFromEntrantStatistics(rawData.getStatistics());
                        List<Float> placePrices = new ArrayList<>();
                        rawData.getPrices().stream().filter(r -> AppConstant.PRICE_CODE.equals(r.getPriceCode())).findFirst().ifPresent(
                                price -> {
                                    if (price.getWinPrice() != null) {
                                        winPrices.add(price.getWinPrice());
                                        CommonUtils.setIfPresent(price.getPlacePrice(), placePrices::add);
                                    }
                                }
                        );
                        Entrant entrant = MeetingMapper.toEntrantEntity(rawData, winPrices, placePrices);
                        if (mapDeductions.containsKey(entrant.getNumber())) {
                            entrant.setCurrentPlaceDeductions(mapDeductions.get(entrant.getNumber()).getPlaceDeduction()/100);
                            entrant.setCurrentWinDeductions(mapDeductions.get(entrant.getNumber()).getWinDeduction()/100);
                        }

                        newEntrants.add(entrant);
                    }

                    return Mono.justOrEmpty(raceDto.getRaceId())
                            .switchIfEmpty(crawUtils.getIdForNewRaceAndSaveRaceSite(raceDto, newEntrants, SiteEnum.SPORT_BET.getId())
                                                    .doOnNext(raceDto::setRaceId)
                            ) // Get id for new race and save race site if raceDto.getRaceId() == null
                            .flatMapMany(raceId -> {
                                if (isRaceStatusFinal(sportBetRaceDto)) {
                                    String top4Entrants = getWinnerEntrants(sportBetRaceDto.getResults())
                                            .limit(4)
                                            .map(resultsRawData -> resultsRawData.getRunnerNumber().toString())
                                            .collect(Collectors.joining(","));
                                    raceDto.setFinalResult(top4Entrants);
                                    crawUtils.updateRaceFinalResultIntoDB(raceDto.getRaceId(), top4Entrants, AppConstant.SPORTBET_SITE_ID);
                                }

                                saveEntrant(newEntrants, raceDto);
                                return Flux.fromIterable(allEntrant.stream().map(EntrantMapper::toEntrantDto).collect(Collectors.toList()));
                            });

                });
    }

    private boolean isRaceStatusFinal(SportBetRaceDto sportBetRaceDto) {
        return ! CollectionUtils.isEmpty(sportBetRaceDto.getResults())
                && SPORT_BET_BETTING_STATUS_RESULTED.equals(sportBetRaceDto.getBettingStatus());
    }

    private Stream<ResultsRawData> getWinnerEntrants(List<ResultsRawData> result) {
        return result.stream().filter(resultsRawData -> resultsRawData.getPlace() != null)
                .sorted(Comparator.comparing(ResultsRawData::getPlace));
    }

    private void saveEntrant(List<Entrant> newEntrants, RaceDto raceDto) {

        crawUtils.saveEntrantCrawlDataToRedis(newEntrants, raceDto, AppConstant.SPORTBET_SITE_ID);
        crawUtils.saveEntrantsPriceIntoDB(newEntrants, raceDto.getRaceId(), AppConstant.SPORTBET_SITE_ID);
    }

    private List<Float> getPricesFromEntrantStatistics(StatisticsRawData statistics) {
        List<Float> prices = new ArrayList<>();
        if (statistics.getOpenPrice() != null) {
            prices.add(statistics.getOpenPrice());
        }
        if (statistics.getFluc1() != null) {
            prices.add(statistics.getFluc1());
        }
        if (statistics.getFluc2() != null) {
            prices.add(statistics.getFluc2());

        }
        return prices;
    }

    private Map<Integer, SportBetDeduction> getEntrantDeductions(List<SportBetDeduction> deductions) {
        Map<Integer, SportBetDeduction> mapDeductions = new HashMap<>();
        if (deductions != null) {
            deductions.forEach(deduction -> mapDeductions.put(deduction.getRunnerNumber(), deduction));
        }
        return mapDeductions;
    }

    private Mono<SportBetRaceDto> crawlEntrantDataSportBet(String raceUUID) {

        String raceQueryURI = AppConstant.SPORT_BET_RACE_QUERY.replace(AppConstant.ID_PARAM, raceUUID);

        return crawUtils.crawlData(sportBetWebClient, raceQueryURI, SportBetRaceApiResponse.class, this.getClass().getName(), 5L)
                        .mapNotNull(SportBetRaceApiResponse::getSportBetRaceDto);
    }

}
