package com.tvf.clb.service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.dto.playup.PlayUpMeetingDto;
import com.tvf.clb.base.dto.playup.PlayUpRaceDto;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.model.*;
import com.tvf.clb.base.model.playup.*;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@ClbService(componentType = AppConstant.PLAY_UP)
@Slf4j
public class PlayUpCrawlService implements ICrawlService{

    @Autowired
    private CrawUtils crawUtils;

    @Autowired
    public WebClient playUpWebClient;

    @Autowired
    public ObjectMapper objectMapper;

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {
        log.info("Start getting the API from PlayUp.");
        String meetingQueryURI = AppConstant.PLAY_UP_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
        String className = this.getClass().getName();

        return crawUtils.crawlData(playUpWebClient, meetingQueryURI, PlayUpMeetingDto.class, className, 5L)
                .doOnError(throwable -> crawUtils.saveFailedCrawlMeeting(className, date))
                .flatMapIterable(playUpMeetingDto -> getAllAusMeeting(playUpMeetingDto, date));
    }

    private List<MeetingDto> getAllAusMeeting(PlayUpMeetingDto playUpMeetingDto, LocalDate date) {
        List<PlayUpMeetingDtoRawData> meetings = playUpMeetingDto.getData();
        List<PlayUpRaceDtoRawData> races = playUpMeetingDto.getIncluded();
        List<MeetingDto> meetingDtoList = new ArrayList<>();
        List<RaceDto> raceDtoList = new ArrayList<>();
        for(PlayUpMeetingDtoRawData playUpMeetingDtoRawData :meetings){
            meetingDtoList.add(MeetingMapper.toMeetingPlayUpDto(playUpMeetingDtoRawData));

        }
        for(PlayUpRaceDtoRawData playUpRaceDtoRawData :races){
            raceDtoList.add(MeetingMapper.toRacePlayUpDto(playUpRaceDtoRawData));

        }

        Map<String, List<RaceDto>> mapMeetingIdAndRaceDto = raceDtoList.stream().collect(Collectors.groupingBy(RaceDto::getMeetingUUID));

        Map<Meeting, List<Race>> mapMeetingAndRace = new HashMap<>();
        meetingDtoList.forEach(meeting -> {
            List<Race> listRaceEntity = mapMeetingIdAndRaceDto.get(meeting.getId()).stream().map(MeetingMapper::toRaceEntity).collect(Collectors.toList());
            mapMeetingAndRace.put(MeetingMapper.toMeetingEntity(meeting), listRaceEntity);
        });

        Mono<Map<String, Long>> mapUUIDToRaceIdMono = crawUtils.saveMeetingSiteAndRaceSite(mapMeetingAndRace, SiteEnum.PLAY_UP.getId());
        setRaceIdThenCrawlAndSaveEntrants(mapUUIDToRaceIdMono, raceDtoList, date);
        return meetingDtoList;
    }

    @Override
    public Mono<CrawlRaceData> getEntrantByRaceUUID(String raceId) {
        return crawlRunnerDataPlayUp(raceId)
                .onErrorResume(throwable -> Mono.empty())
                .map(raceDto -> {
                    List<PlayUpRunnerRawDataDto> playUpRunnerRawDataDtos = raceDto.getIncluded();
                    List<PlayUpRunnerRawDataDto> includedRunners = playUpRunnerRawDataDtos.stream().filter(r->r.getType().equals("selections")).collect(Collectors.toList());

                    List<PlayUpRunnerRawDataDto> includedPrices = playUpRunnerRawDataDtos.stream().filter(r->r.getType().equals("prices")).collect(Collectors.toList());

                    List<PlayUpRunnerRawData> runners = getRunnerRawData(includedRunners, includedPrices);
                    Map<Integer, CrawlEntrantData> mapEtrants = new HashMap<>();
                    runners.forEach(x -> {
                        List<Float> winPrice = new ArrayList<>();
                        winPrice.add(x.getWinPrice());
                        Map<Integer, List<Float>> winPriceFluctuations = new HashMap<>();
                        winPriceFluctuations.put(AppConstant.PLAY_UP_SITE_ID, winPrice);

                        List<Float> placePrice = new ArrayList<>();
                        placePrice.add(x.getPlacePrice());
                        Map<Integer, List<Float>> placePriceFluctuations = new HashMap<>();
                        placePriceFluctuations.put(AppConstant.PLAY_UP_SITE_ID, placePrice);

                        Map<Integer, Float> winDeduction = new HashMap<>();
                        CommonUtils.applyIfPresent(AppConstant.PLAY_UP_SITE_ID,x.getDeductions() ==null ? null: x.getDeductions().getWin(), winDeduction::put);

                        Map<Integer, Float> placeDeduction = new HashMap<>();
                        CommonUtils.applyIfPresent(AppConstant.PLAY_UP_SITE_ID,x.getDeductions() ==null ? null: x.getDeductions().getPlace(), placeDeduction::put);

                        mapEtrants.put(x.getNumber(), new CrawlEntrantData(0, winPriceFluctuations, placePriceFluctuations, winDeduction, placeDeduction));
                    });

                    CrawlRaceData result = new CrawlRaceData();
                    result.setSiteEnum(SiteEnum.PLAY_UP);
                    result.setMapEntrants(mapEtrants);

                    if (isRaceStatusFinal(raceDto)) {
                        String finalResult = raceDto.getData().getAttributes().getResults();
                        result.setFinalResult(Collections.singletonMap(AppConstant.PLAY_UP_SITE_ID, finalResult));
                    }

                    return result;
                });
    }

    private List<PlayUpRunnerRawData> getRunnerRawData(List<PlayUpRunnerRawDataDto> includedRunners, List<PlayUpRunnerRawDataDto> includedPrices) {
        List<PlayUpRunnerRawData> runners = new ArrayList<>();

        List<PlayUpPriceRawData> pricesRawData = getPriceRawData(includedPrices);

        for (PlayUpRunnerRawDataDto includedRunner : includedRunners) {
            try {
                PlayUpRunnerRawData runner = objectMapper.treeToValue(includedRunner.getAttributes(), PlayUpRunnerRawData.class);
                List<String> priceIds = Optional.ofNullable(includedRunner.getRelationships())
                        .map(PlayUpRelationshipsRawData::getPrices)
                        .map(PlayUpPriceRelationshipRawData::getData)
                        .map(playUpDataTypes -> playUpDataTypes.stream().map(PlayUpDataType::getId).collect(Collectors.toList()))
                        .orElse(null);

                if (priceIds != null) {
                    List<PlayUpPriceRawData> runnerPrices = pricesRawData.stream().filter(price -> priceIds.contains(price.getId())).collect(Collectors.toList());
                    for (PlayUpPriceRawData runnerPrice: runnerPrices) {
                        String betType = runnerPrice.getBetType().getName();
                        if (betType.equals("Win")) {
                            runner.setWinPrice(runnerPrice.getDPrice());
                        } else if (betType.equals("Place")) {
                            runner.setPlacePrice(runnerPrice.getDPrice());
                        }
                    }
                }
                runner.setId(includedRunner.getId());
                runners.add(runner);
            } catch (JsonProcessingException e) {
                log.error(e.getMessage());
            }
        }
        return runners;
    }

    private List<PlayUpPriceRawData> getPriceRawData(List<PlayUpRunnerRawDataDto> includePrices) {
        List<PlayUpPriceRawData> prices = new ArrayList<>();

        for (PlayUpRunnerRawDataDto includedPrice : includePrices) {
            try {
                PlayUpPriceRawData price = objectMapper.treeToValue(includedPrice.getAttributes(), PlayUpPriceRawData.class);
                price.setId(includedPrice.getId());
                prices.add(price);
            } catch (JsonProcessingException e) {
                log.error(e.getMessage());
            }
        }
        return prices;
    }

    @Override
    public Flux<EntrantDto> crawlAndSaveEntrantsInRace(RaceDto raceDto, LocalDate date) {

        String raceUUID = raceDto.getId();
        return crawlRunnerDataPlayUp(raceUUID)
                .doOnError(throwable -> crawUtils.saveFailedCrawlRace(this.getClass().getName(), raceDto, date))
                .flatMapMany(playUpRaceDto -> {
                    List<Entrant> newEntrants = new ArrayList<>();
                    List<PlayUpRunnerRawDataDto> playUpRunnerRawDataDtos = playUpRaceDto.getIncluded();

                    List<PlayUpRunnerRawDataDto> includedRunners = playUpRunnerRawDataDtos.stream().filter(r->r.getType().equals("selections")).collect(Collectors.toList());

                    List<PlayUpRunnerRawDataDto> includedPrices = playUpRunnerRawDataDtos.stream().filter(r->r.getType().equals("prices")).collect(Collectors.toList());

                    List<PlayUpRunnerRawData> runners = getRunnerRawData(includedRunners, includedPrices);
                    runners.forEach(x -> {
                        List<Float> winPriceFluctuations = null;
                        winPriceFluctuations.add(x.getWinPrice());

                        List<Float> placePriceFluctuations = null;
                        placePriceFluctuations.add(x.getPlacePrice());

                        Entrant entrant = EntrantMapper.toEntrantEntityPlayUp(x,winPriceFluctuations, placePriceFluctuations, raceUUID);
                        newEntrants.add(entrant);
                    });


                    return Mono.justOrEmpty(raceDto.getRaceId())
                            .switchIfEmpty(crawUtils.getIdForNewRaceAndSaveRaceSite(raceDto, newEntrants, SiteEnum.SPORT_BET.getId())
                                    .doOnNext(raceDto::setRaceId)
                            ) // Get id for new race and save race site if raceDto.getRaceId() == null
                            .flatMapMany(raceId -> {
                                if (isRaceStatusFinal(playUpRaceDto)) {
                                    String top4Entrants =playUpRaceDto.getData().getAttributes().getResults();
                                    raceDto.setFinalResult(top4Entrants);
                                    crawUtils.updateRaceFinalResultIntoDB(raceDto.getRaceId(), top4Entrants, AppConstant.PLAY_UP_SITE_ID);
                                }

                                saveEntrant(newEntrants, raceDto);
                                return Flux.fromIterable(runners.stream().map(EntrantMapper::toEntrantDto).collect(Collectors.toList()));
                            });

                });
    }

    private boolean isRaceStatusFinal(PlayUpRaceDto raceDto) {
        String status = raceDto.getData().getAttributes().getRaceStatus().getName();
        return AppConstant.PLAY_UP_RACE_STATUS_FINAL.equalsIgnoreCase(status);
    }

    private void saveEntrant(List<Entrant> newEntrants, RaceDto raceDto) {
        crawUtils.saveEntrantCrawlDataToRedis(newEntrants, raceDto, AppConstant.PLAY_UP_SITE_ID);
        crawUtils.saveEntrantsPriceIntoDB(newEntrants, raceDto.getRaceId(), AppConstant.PLAY_UP_SITE_ID);
    }
    private Mono<PlayUpRaceDto> crawlRunnerDataPlayUp(String raceId) {
        String raceQueryURI = AppConstant.PLAY_UP_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
        return crawUtils.crawlData(playUpWebClient, raceQueryURI, PlayUpRaceDto.class, this.getClass().getName(), 5L);
    }

}
