package com.tvf.clb.service.service;

import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.dto.tab.TabBetMeetingDto;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.model.CrawlEntrantData;
import com.tvf.clb.base.model.CrawlRaceData;
import com.tvf.clb.base.model.EntrantRawData;
import com.tvf.clb.base.model.tab.*;
import com.tvf.clb.base.utils.AppConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@ClbService(componentType = AppConstant.TAB)
@Slf4j
public class TabCrawlService implements ICrawlService{

    @Autowired
    private CrawUtils crawUtils;

    @Autowired
    public WebClient tabWebClient;

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {
        log.info("Start getting the API from TAB.");
        String meetingQueryURI = AppConstant.TAB_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
        String className = this.getClass().getName();

        return crawUtils.crawlData(tabWebClient, meetingQueryURI, TabBetMeetingDto.class, className, 5L)
                .doOnError(throwable -> crawUtils.saveFailedCrawlMeeting(className, date))
                .mapNotNull(TabBetMeetingDto::getMeetings)
                .flatMapIterable(tabMeetingRawData -> getAllAusMeeting(tabMeetingRawData, date));
    }

    private List<MeetingDto> getAllAusMeeting(List<TabMeetingRawData> meetingRawData,LocalDate date) {
        log.info("[TAB]  Sum all meeting: {}", meetingRawData.size());
        log.info("[TAB] Sum all race: {}", meetingRawData.stream().mapToInt(m -> m.getRaces().size()).sum());
        List<MeetingDto> meetingDtoList = new ArrayList<>();
        Map<Meeting, List<Race>> mapMeetingAndRace = new HashMap<>();
        for (TabMeetingRawData localMeeting : meetingRawData) {
            List<TabRacesData> localRace =localMeeting.getRaces();
            MeetingDto meetingDto =MeetingMapper.toMeetingTABDto(localMeeting,localRace);
            meetingDtoList.add(meetingDto);
            mapMeetingAndRace.put(MeetingMapper.toMeetingEntity(meetingDto), meetingDto.getRaces().stream().filter(race -> race.getNumber() != null).map(MeetingMapper::toRaceEntity).collect(Collectors.toList()));
        }

        List<RaceDto> raceDtoList = meetingDtoList.stream().map(MeetingDto::getRaces).flatMap(List::stream).collect(Collectors.toList());
        Mono<Map<String, Long>> mapUUIDToRaceIdMono = crawUtils.saveMeetingSiteAndRaceSite(mapMeetingAndRace, SiteEnum.TAB.getId());

        setRaceIdThenCrawlAndSaveEntrants(mapUUIDToRaceIdMono, raceDtoList, date);

        return meetingDtoList;
    }

    @Override
    public Mono<CrawlRaceData> getEntrantByRaceUUID(String raceId) {
        return crawlRunnerDataTAB(raceId)
                .onErrorResume(throwable -> Mono.empty())
                .filter(apiResponse -> apiResponse.getRunners() != null)
                .map(raceDto -> {

                    List<EntrantRawData> allEntrant = getListEntrant(raceId, raceDto);

                    Map<Integer, CrawlEntrantData> mapEtrants = new HashMap<>();
                    allEntrant.forEach(x -> {
                        Map<Integer, List<Float>> winPriceFluctuations = new HashMap<>();
                        winPriceFluctuations.put(AppConstant.TAB_SITE_ID, Optional.ofNullable(x.getPriceFluctuations()).orElse(new ArrayList<>()));

                        Map<Integer, List<Float>> placePriceFluctuations = new HashMap<>();
                        placePriceFluctuations.put(AppConstant.TAB_SITE_ID, Optional.ofNullable(x.getPricePlaces()).orElse(new ArrayList<>()));

                        mapEtrants.put(x.getNumber(), new CrawlEntrantData(x.getPosition(), winPriceFluctuations, placePriceFluctuations));
                    });

                    CrawlRaceData result = new CrawlRaceData();
                    result.setSiteEnum(SiteEnum.TAB);
                    result.setMapEntrants(mapEtrants);

                    if (isRaceStatusFinal(raceDto)) {
                        String finalResult = raceDto.getResults().stream().map(Object::toString).collect(Collectors.joining(","));
                        result.setFinalResult(Collections.singletonMap(AppConstant.TAB_SITE_ID, finalResult));
                    }

                    return result;
                });
    }

    @Override
    public Flux<EntrantDto> crawlAndSaveEntrantsInRace(RaceDto raceDto, LocalDate date) {

        String raceUUID = raceDto.getId();

        return crawlRunnerDataTAB(raceUUID)
                .doOnError(throwable -> crawUtils.saveFailedCrawlRace(this.getClass().getName(), raceDto, date))
                .filter(result -> result.getRunners() != null)
                .flatMapMany(runnerRawData -> {
                    List<EntrantRawData> listEntrantRawData = getListEntrant(raceUUID, runnerRawData);
                    List<Entrant> listEntrantEntity = listEntrantRawData.stream().distinct().map(MeetingMapper::toEntrantEntity).collect(Collectors.toList());

                    return Mono.justOrEmpty(raceDto.getRaceId())
                               .switchIfEmpty(crawUtils.getIdForNewRaceAndSaveRaceSite(raceDto, listEntrantEntity, SiteEnum.TAB.getId())
                                                       .doOnNext(raceDto::setRaceId)
                               ) // Get id for new race and save race site if raceDto.getRaceId() == null
                               .flatMapIterable(raceId -> {
                                   if (isRaceStatusFinal(runnerRawData)) {
                                       String finalResult = runnerRawData.getResults().stream().map(Object::toString).collect(Collectors.joining(","));
                                       raceDto.setDistance(runnerRawData.getRaceDistance());
                                       raceDto.setFinalResult(finalResult);
                                       crawUtils.updateRaceFinalResultIntoDB(raceId, finalResult, AppConstant.TAB_SITE_ID);
                                   }
                                   raceDto.setDistance(runnerRawData.getRaceDistance());
                                   saveEntrant(listEntrantEntity, raceDto);

                                   return listEntrantRawData.stream().map(EntrantMapper::toEntrantDto).collect(Collectors.toList());
                               });
                });
    }

    private boolean isRaceStatusFinal(TabRunnerRawData runnerRawData) {
        return runnerRawData.getRaceStatus() != null
                && AppConstant.TAB_RACE_STATUS_FINAL.equalsIgnoreCase(runnerRawData.getRaceStatus());
    }

    private void saveEntrant(List<Entrant> newEntrants, RaceDto raceDto) {
        crawUtils.saveEntrantCrawlDataToRedis(newEntrants, raceDto, AppConstant.TAB_SITE_ID);
        crawUtils.saveEntrantsPriceIntoDB(newEntrants, raceDto.getRaceId(), AppConstant.TAB_SITE_ID);
    }

    private List<EntrantRawData> getListEntrant(String raceId, TabRunnerRawData runnerRawData) {
        return runnerRawData.getRunners().stream().filter(f -> f.getFixedOdds() != null)
                .map(x -> {
                    List<Float> listWinPrice = x.getFixedOdds().getFlucs() == null ? new ArrayList<>() :
                            x.getFixedOdds().getFlucs().stream().map(TabPriceFlucsRawData::getReturnWin).filter(aFloat -> aFloat != null && !aFloat.equals(0.0F))
                            .collect(Collectors.toList());
                    return EntrantMapper.toEntrantRawData(x, runnerRawData.getResults(), listWinPrice, x.getFixedOdds().getReturnPlace(),  raceId);
                }).collect(Collectors.toList());
    }

    private Mono<TabRunnerRawData> crawlRunnerDataTAB(String raceId) {
        String raceQueryURI = AppConstant.TAB_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
        return crawUtils.crawlData(tabWebClient, raceQueryURI, TabRunnerRawData.class, this.getClass().getName(), 5L);
    }

}
