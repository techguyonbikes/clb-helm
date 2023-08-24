package com.tvf.clb.service.service;

import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.model.CrawlEntrantData;
import com.tvf.clb.base.model.CrawlRaceData;
import com.tvf.clb.base.model.colossalbet.*;
import com.tvf.clb.base.utils.AppConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ClbService(componentType = AppConstant.COLOSSAL_BET)
@Slf4j
@RequiredArgsConstructor
public class ColossalBetCrawlService implements ICrawlService {

    private final CrawUtils crawUtils;

    private final WebClient colBetWebClientRace;
    private final WebClient colBetWebClientMeeting;

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {
        log.info("Start getting the API from ColossalBet");
        String meetingQueryURI = AppConstant.COLOSSAL_BET_MEETING_QUERY.replace(AppConstant.DATE_PARAM,date.toString());
        String className = this.getClass().getName();

        return crawUtils.crawlData(colBetWebClientMeeting, meetingQueryURI, ColBetMeetingApiResponse.class, className, 5L)
                .doOnError(throwable -> crawUtils.saveFailedCrawlMeeting(className, date))
                .mapNotNull(ColBetMeetingApiResponse::getMeeting)
                .flatMapIterable(meetings -> getAllMeetings(meetings, date));
    }


    private List<MeetingDto> getAllMeetings(List<ColBetMeetingRawData> meetingRawDataList, LocalDate date) {
        List<MeetingDto> meetingDtoList = meetingRawDataList.stream().map(MeetingMapper::toMeetingDto).collect(Collectors.toList());
        Map<Meeting, List<Race>> mapMeetingAndRace = new HashMap<>();
        for (MeetingDto meetingDto : meetingDtoList) {
            mapMeetingAndRace.put(MeetingMapper.toMeetingEntity(meetingDto), meetingDto.getRaces().stream().map(MeetingMapper::toRaceEntity).collect(Collectors.toList()));
        }
        log.info("Number of meetings: {}", mapMeetingAndRace.keySet().size());
        log.info("Number of races: {}", mapMeetingAndRace.values().stream().mapToLong(List::size).sum());

        List<RaceDto> raceDtoList = meetingDtoList.stream().map(MeetingDto::getRaces).flatMap(List::stream).collect(Collectors.toList());
        Mono<Map<String, Long>> mapUUIDToRaceIdMono = crawUtils.saveMeetingSiteAndRaceSite(mapMeetingAndRace, SiteEnum.COLOSSAL_BET.getId());

        setRaceIdThenCrawlAndSaveEntrants(mapUUIDToRaceIdMono, raceDtoList, date);

        return meetingDtoList;
    }

    @Override
    public Mono<CrawlRaceData> getEntrantByRaceUUID(String raceUUID) {
        return crawlColBetRaceData(raceUUID)
                .onErrorResume(throwable -> Mono.empty())
                .map(raceRawData -> {

                    CrawlRaceData result = new CrawlRaceData();
                    result.setSiteEnum(SiteEnum.COLOSSAL_BET);

                    Map<Integer, CrawlEntrantData> mapEntrants = new HashMap<>();

                    for (ColBetRunnerRawData entrantRawData: raceRawData.getRunners()) {
                        mapEntrants.put(entrantRawData.getNumber(), EntrantMapper.toCrawlEntrantData(entrantRawData));
                    }
                    result.setMapEntrants(mapEntrants);

                    String raceStatus = raceRawData.getStatus();
                    if (AppConstant.COLOSSAL_BET_RACE_STATUS_FINAL.equals(raceStatus)) {
                        result.setFinalResult(Collections.singletonMap(AppConstant.COLOSSAL_BET_SITE_ID, raceRawData.getResult()));
                    }

                    return result;
                });
    }

    @Override
    public Flux<EntrantDto> crawlAndSaveEntrantsInRace(RaceDto raceDto, LocalDate date) {
        String raceUUID = raceDto.getId();

        return crawlColBetRaceData(raceUUID)
                .doOnError(throwable -> crawUtils.saveFailedCrawlRace(this.getClass().getName(), raceDto, date))
                .flatMapMany(raceRawData -> {
                    // Convert to entity from raw data, include: basic info and all prices
                    List<Entrant> entrants = raceRawData.getRunners().stream().map(EntrantMapper::toEntrantEntity).collect(Collectors.toList());
                    return Mono.justOrEmpty(raceDto.getRaceId())
                            .switchIfEmpty(crawUtils.getIdForNewRaceAndSaveRaceSite(raceDto, entrants, SiteEnum.COLOSSAL_BET.getId())
                                    .doOnNext(raceDto::setRaceId)
                            ) // Get id for new race and save race site if raceDto.getRaceId() == null
                            .flatMapIterable(raceId -> {
                                String raceStatus = raceRawData.getStatus();

                                // Check race status, final result then save
                                if (AppConstant.COLOSSAL_BET_RACE_STATUS_FINAL.equals(raceStatus)) {
                                    String finalResult = raceRawData.getResult();
                                    raceDto.setFinalResult(finalResult);
                                    crawUtils.updateRaceFinalResultIntoDB(raceDto.getRaceId(), finalResult, AppConstant.COLOSSAL_BET_SITE_ID);
                                }

                                crawUtils.saveEntrantCrawlDataToRedis(entrants, raceDto, AppConstant.COLOSSAL_BET_SITE_ID);
                                crawUtils.saveEntrantsPriceIntoDB(entrants, raceId, AppConstant.COLOSSAL_BET_SITE_ID);

                                return entrants.stream().map(EntrantMapper::toEntrantDto).collect(Collectors.toList());
                            });
                });
    }

    public Mono<ColBetRaceDetailRawData> crawlColBetRaceData(String raceUUID) {
        String raceQueryURI = AppConstant.COLOSSAL_BET_RACE_QUERY.replace(AppConstant.ID_PARAM, raceUUID);

        return crawUtils.crawlData(colBetWebClientRace, raceQueryURI, ColBetRaceDetailRawData.class, this.getClass().getName(), 5L);
    }
}
