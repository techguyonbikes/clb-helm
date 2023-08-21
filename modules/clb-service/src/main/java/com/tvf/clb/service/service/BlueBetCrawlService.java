package com.tvf.clb.service.service;

import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.model.CrawlEntrantData;
import com.tvf.clb.base.model.CrawlRaceData;
import com.tvf.clb.base.model.bluebet.*;
import com.tvf.clb.base.utils.AppConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@ClbService(componentType = AppConstant.BLUE_BET)
@Slf4j
@RequiredArgsConstructor
public class BlueBetCrawlService implements ICrawlService {

    private final CrawUtils crawUtils;

    private final WebClient blueBetWebClient;

    private static final String WIN_MARKET = "WIN";
    private static final String PLACE_MARKET = "PLC";

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {
        log.info("Start getting the API from BlueBet.");

        String meetingQueryURI = AppConstant.BLUE_BET_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
        String className = this.getClass().getName();

        return crawUtils.crawlData(blueBetWebClient, meetingQueryURI, BlueBetMeetingApiResponse.class, className, 5L)
                .doOnError(throwable -> crawUtils.saveFailedCrawlMeeting(className, date))
                .flatMapIterable(meetingApiResponse -> getAllMeeting(meetingApiResponse, date));
    }

    private List<MeetingDto> getAllMeeting(BlueBetMeetingApiResponse meetingApiResponse, LocalDate date) {
        // get all meeting
        List<MeetingDto> meetingDtoList = new ArrayList<>();
        if (meetingApiResponse.getGreyhoundRacing() != null) {
            meetingDtoList.addAll(meetingApiResponse.getGreyhoundRacing().stream()
                                                    .map(races -> toMeetingDtoFromRawData(races, AppConstant.GREYHOUND_RACING))
                                                    .filter(Objects::nonNull)
                                                    .collect(Collectors.toList()));
        }
        if (meetingApiResponse.getGreyhoundRacing() != null) {
            meetingDtoList.addAll(meetingApiResponse.getHorseRacing().stream()
                                                    .map(races -> toMeetingDtoFromRawData(races, AppConstant.HORSE_RACING))
                                                    .filter(Objects::nonNull)
                                                    .collect(Collectors.toList()));
        }
        if (meetingApiResponse.getGreyhoundRacing() != null) {
            meetingDtoList.addAll(meetingApiResponse.getHarnessRacing().stream()
                                                    .map(races -> toMeetingDtoFromRawData(races, AppConstant.HARNESS_RACING))
                                                    .filter(Objects::nonNull)
                                                    .collect(Collectors.toList()));
        }

        Map<Meeting, List<Race>> mapMeetingAndRace = new HashMap<>();
        for (MeetingDto meetingDto : meetingDtoList) {
            mapMeetingAndRace.put(MeetingMapper.toMeetingEntity(meetingDto), meetingDto.getRaces().stream().map(MeetingMapper::toRaceEntity).collect(Collectors.toList()));
        }

        log.info("Number of meeting: {}", mapMeetingAndRace.keySet().size());
        log.info("Number of race: {}", mapMeetingAndRace.values().stream().mapToLong(List::size).sum());

        List<RaceDto> raceDtoList = meetingDtoList.stream().map(MeetingDto::getRaces).flatMap(List::stream).collect(Collectors.toList());
        Mono<Map<String, Long>> mapUUIDToRaceIdMono = crawUtils.saveMeetingSiteAndRaceSite(mapMeetingAndRace, AppConstant.BLUE_BET_SITE_ID);

        setRaceIdThenCrawlAndSaveEntrants(mapUUIDToRaceIdMono, raceDtoList, date);

        return meetingDtoList;
    }

    @Override
    public Mono<CrawlRaceData> getEntrantByRaceUUID(String raceUUID) {
        return crawlBlueBetRaceData(raceUUID)
                .onErrorResume(throwable -> Mono.empty())
                .map(raceRawData -> {
                    CrawlRaceData result = new CrawlRaceData();
                    result.setSiteEnum(SiteEnum.BLUE_BET);

                    Map<Integer, CrawlEntrantData> mapEntrants = new HashMap<>();

                    Map<Integer, BlueBetDeductionRawData> mapEntrantNumberAndDeductions = getMapEntrantNumberAndDeductions(raceRawData.getResults());
                    for (BlueBetEntrantRawData entrantRawData : raceRawData.getEntrants()) {
                        Integer number = entrantRawData.getNumber();
                        mapEntrants.put(number, convertFromRawDataToCrawlEntrantData(entrantRawData, mapEntrantNumberAndDeductions.get(number)));
                    }
                    result.setMapEntrants(mapEntrants);

                    Optional<BlueBetRaceReferenceRawData> raceRefOptional = getRaceReferenceByNumber(raceRawData.getRaceReferences(), raceRawData.getNumber());
                    if (raceRefOptional.isPresent()) {
                        BlueBetRaceReferenceRawData raceRef = raceRefOptional.get();
                        String raceStatus = getRaceStatus(raceRef.getIsOpenForBetting(), raceRef.getResultStatusDesc());
                        result.setStatus(raceStatus);
                        if (AppConstant.STATUS_FINAL.equals(raceStatus)) {
                            result.setFinalResult(Collections.singletonMap(AppConstant.BLUE_BET_SITE_ID, raceRef.getPlacing()));
                        }
                    }

                    return result;
                });
    }

    @Override
    public Flux<EntrantDto> crawlAndSaveEntrantsInRace(RaceDto raceDto, LocalDate date) {
        String raceUUID = raceDto.getId();
        return crawlBlueBetRaceData(raceUUID)
                .doOnError(throwable -> crawUtils.saveFailedCrawlRace(this.getClass().getName(), raceDto, date))
                .flatMapMany(raceRawData -> {
                    List<Entrant> listEntrantEntity = toListEntrantEntityFromRaceRawData(raceRawData);

                    return Mono.justOrEmpty(raceDto.getRaceId())
                            .switchIfEmpty(crawUtils.getIdForNewRaceAndSaveRaceSite(raceDto, listEntrantEntity, SiteEnum.BLUE_BET.getId())
                                                    .doOnNext(raceDto::setRaceId)
                            ) // Get id for new race and save race site if raceDto.getRaceId() == null
                            .flatMapIterable(raceId -> {

                                Optional<BlueBetRaceReferenceRawData> raceRefOptional = getRaceReferenceByNumber(raceRawData.getRaceReferences(), raceRawData.getNumber());
                                if (raceRefOptional.isPresent()) {
                                    BlueBetRaceReferenceRawData raceRef = raceRefOptional.get();
                                    String raceStatus = getRaceStatus(raceRef.getIsOpenForBetting(), raceRef.getResultStatusDesc());
                                    raceDto.setStatus(raceStatus);
                                    if (AppConstant.STATUS_FINAL.equals(raceStatus)) {
                                        crawUtils.updateRaceFinalResultIntoDB(raceId, raceRef.getPlacing(), AppConstant.BLUE_BET_SITE_ID);
                                    }
                                }

                                crawUtils.saveEntrantCrawlDataToRedis(listEntrantEntity, raceDto, AppConstant.BLUE_BET_SITE_ID);
                                crawUtils.saveEntrantsPriceIntoDB(listEntrantEntity, raceDto.getRaceId(), AppConstant.BLUE_BET_SITE_ID);

                                return listEntrantEntity.stream().map(EntrantMapper::toEntrantDto).collect(Collectors.toList());
                            });
                });
    }

    private List<Entrant> toListEntrantEntityFromRaceRawData(BlueBetRaceDetailRawData raceRawData) {
        List<Entrant> result = new ArrayList<>();
        Map<Integer, BlueBetDeductionRawData> mapEntrantNumberAndDeductions = getMapEntrantNumberAndDeductions(raceRawData.getResults());

        for (BlueBetEntrantRawData entrantRawData : raceRawData.getEntrants()) {
            Float winDeduction = null;
            Float placeDeduction = null;
            if (mapEntrantNumberAndDeductions.containsKey(entrantRawData.getNumber())) {
                BlueBetDeductionRawData deductionRawData = mapEntrantNumberAndDeductions.get(entrantRawData.getNumber());
                winDeduction = deductionRawData.getWin();
                placeDeduction = getPlaceDeduction(deductionRawData);
            }
            Entrant entrant = Entrant.builder()
                                .name(entrantRawData.getName())
                                .number(entrantRawData.getNumber())
                                .isScratched(entrantRawData.getScratched())
                                .currentSitePrice(getEntrantPrice(entrantRawData.getPrices(), WIN_MARKET))
                                .currentSitePricePlaces(getEntrantPrice(entrantRawData.getPrices(), PLACE_MARKET))
                                .currentWinDeductions(winDeduction)
                                .currentPlaceDeductions(placeDeduction)
                                .build();
            result.add(entrant);
        }

        return result;
    }

    public static MeetingDto toMeetingDtoFromRawData(List<BlueBetRaceRawData> races, String raceType) {
        if (CollectionUtils.isEmpty(races)) return null;

        String meetingName = races.get(0).getMeetingName();
        return MeetingDto.builder()
                .id(UUID.randomUUID().toString())
                .name(meetingName)
                .raceType(raceType)
                .races(races.stream().map(race -> toRaceDtoFromRawData(race, raceType)).collect(Collectors.toList()))
                .build();
    }

    public static RaceDto toRaceDtoFromRawData(BlueBetRaceRawData race, String raceType) {
        String url = String.format("%s/%s/Race-%d/%d", race.getMasterCategoryName().replace(" ", "-"), race.getMeetingName(), race.getNumber(), race.getId());
        return RaceDto.builder()
                .id(race.getId().toString())
                .name(race.getName())
                .advertisedStart(race.getAdvertisedStartTime())
                .number(race.getNumber())
                .meetingName(race.getMeetingName())
                .finalResult(StringUtils.hasText(race.getResults()) ? race.getResults() : null)
                .raceSiteUrl(AppConstant.URL_BLUEBET_RACE.replace(AppConstant.ID_PARAM, url))
                .raceType(raceType)
                .build();
    }

    private Mono<BlueBetRaceDetailRawData> crawlBlueBetRaceData(String uuid) {
        String raceQueryURI = AppConstant.BLUE_BET_RACE_QUERY.replace(AppConstant.ID_PARAM, uuid);
        return crawUtils.crawlData(blueBetWebClient, raceQueryURI, BlueBetRaceDetailRawData.class, this.getClass().getName(), 5L)
                        .filter(blueBetRaceDetailRawData -> !CollectionUtils.isEmpty(blueBetRaceDetailRawData.getEntrants()));
    }

    private Optional<BlueBetRaceReferenceRawData> getRaceReferenceByNumber(List<BlueBetRaceReferenceRawData> raceReferences, Integer raceNumber) {
        return raceReferences.stream().filter(raceReference -> raceReference.getRaceNumber().equals(raceNumber)).findFirst();
    }

    private Map<Integer, BlueBetDeductionRawData> getMapEntrantNumberAndDeductions(BlueBetRaceResultRawData raceResult) {
        if (raceResult == null || CollectionUtils.isEmpty(raceResult.getDeductions())) {
            return Collections.emptyMap();
        }
        Map<Integer, BlueBetDeductionRawData> result = new HashMap<>();
        raceResult.getDeductions().forEach(deduction -> result.put(deduction.getEntrantNumber(), deduction));

        return result;
    }

    private CrawlEntrantData convertFromRawDataToCrawlEntrantData(BlueBetEntrantRawData entrantRawData, BlueBetDeductionRawData deductionRawData) {
        Map<Integer, List<Float>> winPrices = new HashMap<>();
        winPrices.put(AppConstant.BLUE_BET_SITE_ID, getEntrantPrice(entrantRawData.getPrices(), WIN_MARKET));

        Map<Integer, List<Float>> placePrices = new HashMap<>();
        placePrices.put(AppConstant.BLUE_BET_SITE_ID, getEntrantPrice(entrantRawData.getPrices(), PLACE_MARKET));

        Map<Integer, Float> winDeductions = new HashMap<>();
        Map<Integer, Float> placeDeductions = new HashMap<>();

        if (deductionRawData != null) {
            if (deductionRawData.getWin() != null) {
                winDeductions.put(AppConstant.BLUE_BET_SITE_ID, deductionRawData.getWin());
            }
            Float placeDeduction = getPlaceDeduction(deductionRawData);
            if (placeDeduction != null) {
                placeDeductions.put(AppConstant.BLUE_BET_SITE_ID, placeDeduction);
            }
        }

        return new CrawlEntrantData(0, winPrices, placePrices, winDeductions, placeDeductions);
    }

    private List<Float> getEntrantPrice(List<BlueBetPriceRawData> prices, String marketType) {
        List<Float> result = new ArrayList<>(1);
        if (!CollectionUtils.isEmpty(prices)) {
            prices.stream().filter(priceRawData -> marketType.equals(priceRawData.getMarketTypeCode()) && ! priceRawData.getPrice().equals(0F))
                           .findFirst()
                           .ifPresent(priceRawData -> result.add(priceRawData.getPrice()));
        }
        return result;
    }

    private Float getPlaceDeduction(@NonNull BlueBetDeductionRawData deductionRawData) {
        if (deductionRawData.getPlace1() != null) return deductionRawData.getPlace1();
        if (deductionRawData.getPlace2() != null) return deductionRawData.getPlace2();
        if (deductionRawData.getPlace3() != null) return deductionRawData.getPlace3();
        return null;
    }

    public static String getRaceStatus(Boolean isOpenForBetting, String statusDescription) {
        if (Boolean.TRUE.equals(isOpenForBetting)) {
            return AppConstant.STATUS_OPEN;
        } else {
            switch (statusDescription) {
                case "Closed":
                    return AppConstant.STATUS_CLOSED;
                case "Interim":
                    return AppConstant.STATUS_INTERIM;
                case "Correct Weight":
                    return AppConstant.STATUS_FINAL;
                case "Abandoned":
                    return AppConstant.STATUS_ABANDONED;
                default:
                    return null;
            }
        }
    }
}
