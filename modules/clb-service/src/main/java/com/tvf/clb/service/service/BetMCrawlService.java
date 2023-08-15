package com.tvf.clb.service.service;

import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.model.CrawlEntrantData;
import com.tvf.clb.base.model.CrawlRaceData;
import com.tvf.clb.base.model.betm.*;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.ConvertBase;
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

@ClbService(componentType = AppConstant.BET_M)
@Slf4j
@RequiredArgsConstructor
public class BetMCrawlService implements ICrawlService {

    private final CrawUtils crawUtils;

    private final WebClient betMWebClient;

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {
        log.info("Start getting the API from BetM.");
        String meetingQueryURI = AppConstant.BET_M_MEETING_QUERY.replace(AppConstant.DATE_PARAM, ConvertBase.getDateOfWeek(date));
        String className = this.getClass().getName();

        return crawUtils.crawlData(betMWebClient, meetingQueryURI, BetMMeetingApiResponse.class, className, 5L)
                .doOnError(throwable -> crawUtils.saveFailedCrawlMeeting(className, date))
                .mapNotNull(BetMMeetingApiResponse::getMeetings)
                .flatMapIterable(meetings -> getAllMeetings(meetings, date));
    }


    private List<MeetingDto> getAllMeetings(List<BetMMeetingRawData> meetingRawDataList, LocalDate date) {
        // Convert to MeetingDto, RaceDto (include raceSiteUrl) from raw data
        List<MeetingDto> meetingDtoList = meetingRawDataList.stream().map(MeetingMapper::toMeetingDto).collect(Collectors.toList());

        Map<Meeting, List<Race>> mapMeetingAndRace = new HashMap<>();
        for (MeetingDto meetingDto : meetingDtoList) {
            mapMeetingAndRace.put(MeetingMapper.toMeetingEntity(meetingDto), meetingDto.getRaces().stream().map(MeetingMapper::toRaceEntity).collect(Collectors.toList()));
        }
        log.info("Number of meetings: {}", mapMeetingAndRace.keySet().size());
        log.info("Number of races: {}", mapMeetingAndRace.values().stream().mapToLong(List::size).sum());

        List<RaceDto> raceDtoList = meetingDtoList.stream().map(MeetingDto::getRaces).flatMap(List::stream).collect(Collectors.toList());
        Mono<Map<String, Long>> mapUUIDToRaceIdMono = crawUtils.saveMeetingSiteAndRaceSite(mapMeetingAndRace, SiteEnum.BET_M.getId());

        setRaceIdThenCrawlAndSaveEntrants(mapUUIDToRaceIdMono, raceDtoList, date);

        return meetingDtoList;
    }

    @Override
    public Mono<CrawlRaceData> getEntrantByRaceUUID(String raceUUID) {
        return crawlBetMRaceData(raceUUID)
                .onErrorResume(throwable -> Mono.empty())
                .map(raceRawData -> {

                    CrawlRaceData result = new CrawlRaceData();
                    result.setSiteEnum(SiteEnum.BET_M);

                    Map<Integer, CrawlEntrantData> mapEntrants = new HashMap<>();

                    for (BetMRunnerRawData entrantRawData: raceRawData.getRunners()) {
                        mapEntrants.put(entrantRawData.getNumber(), EntrantMapper.toCrawlEntrantData(entrantRawData));
                    }
                    result.setMapEntrants(mapEntrants);

                    String raceStatus = BetMRaceStatusEnum.getValueFromRawData(raceRawData.getStatus());
                    if (AppConstant.STATUS_FINAL.equals(raceStatus)) {
                        result.setFinalResult(Collections.singletonMap(AppConstant.BET_M_SITE_ID, getFinalResult(raceRawData.getResult())));
                    }

                    return result;
                });
    }

    @Override
    public Flux<EntrantDto> crawlAndSaveEntrantsInRace(RaceDto raceDto, LocalDate date) {
        String raceUUID = raceDto.getId();

        return crawlBetMRaceData(raceUUID)
                .doOnError(throwable -> crawUtils.saveFailedCrawlRace(this.getClass().getName(), raceDto, date))
                .flatMapMany(raceRawData -> {
                    // Convert to entity from raw data, include: basic info and all prices
                    List<Entrant> entrants = raceRawData.getRunners().stream().map(EntrantMapper::toEntrantEntity).collect(Collectors.toList());
                    raceDto.setMeetingName(raceRawData.getMeetingName());

                    return Mono.justOrEmpty(raceDto.getRaceId())
                            .switchIfEmpty(crawUtils.getIdForNewRaceAndSaveRaceSite(raceDto, entrants, SiteEnum.BET_M.getId())
                                    .doOnNext(raceDto::setRaceId)
                            ) // Get id for new race and save race site if raceDto.getRaceId() == null
                            .flatMapIterable(raceId -> {
                                String raceStatus = BetMRaceStatusEnum.getValueFromRawData(raceRawData.getStatus());

                                // Check race status, final result then save
                                if (AppConstant.STATUS_FINAL.equals(raceStatus)) {
                                    String finalResult = getFinalResult(raceRawData.getResult());
                                    raceDto.setFinalResult(finalResult);
                                    crawUtils.updateRaceFinalResultIntoDB(raceDto.getRaceId(), finalResult, AppConstant.BET_M_SITE_ID);
                                }

                                crawUtils.saveEntrantCrawlDataToRedis(entrants, raceDto, AppConstant.BET_M_SITE_ID);
                                crawUtils.saveEntrantsPriceIntoDB(entrants, raceId, AppConstant.BET_M_SITE_ID);

                                return entrants.stream().map(EntrantMapper::toEntrantDto).collect(Collectors.toList());
                            });
                });
    }

    public Mono<BetMRaceDetailRawData> crawlBetMRaceData(String raceUUID) {
        String raceQueryURI = AppConstant.BET_M_RACE_QUERY.replace(AppConstant.ID_PARAM, raceUUID);

        return crawUtils.crawlData(betMWebClient, raceQueryURI, BetMRaceApiResponse.class, this.getClass().getName(), 5L)
                        .mapNotNull(BetMRaceApiResponse::getRace);
    }

    private static String getFinalResult(String resultRawData) {
        return resultRawData == null ? null : resultRawData.replace('/', ',').replace(",,", ",");
    }
}
