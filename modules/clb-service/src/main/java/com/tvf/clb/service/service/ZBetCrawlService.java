package com.tvf.clb.service.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.model.CrawlEntrantData;
import com.tvf.clb.base.model.CrawlRaceData;
import com.tvf.clb.base.model.EntrantRawData;
import com.tvf.clb.base.model.zbet.*;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.ConvertBase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@ClbService(componentType = AppConstant.ZBET)
@Slf4j
public class ZBetCrawlService implements ICrawlService {

    @Autowired
    private CrawUtils crawUtils;

    @Autowired
    public WebClient zbetsWebClient;

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {
        log.info("Start getting the API from ZBet.");
        String meetingQueryURI = AppConstant.ZBET_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
        String className = this.getClass().getName();

        return crawUtils.crawlData(zbetsWebClient, meetingQueryURI, ZBetMeetingResponseRawData.class, className, 20L)
                        .doOnError(throwable -> crawUtils.saveFailedCrawlMeeting(className, date))
                        .mapNotNull(ZBetMeetingResponseRawData::getData)
                        .flatMapIterable(zBetMeetingRawData -> getAllAusMeeting(zBetMeetingRawData, date));
    }

    @Override
    public Mono<CrawlRaceData> getEntrantByRaceUUID(String raceId) {
        return getZBetRaceData(raceId)
                .onErrorResume(throwable -> Mono.empty())
                .map(zBetRaceRawData -> {
                    List<EntrantRawData> allEntrant = getListEntrant(raceId, zBetRaceRawData);

                    Map<Integer, CrawlEntrantData> mapEntrants = new HashMap<>();
                    allEntrant.forEach(x -> mapEntrants.put(x.getNumber(), EntrantMapper.toCrawlEntrantData(x, AppConstant.ZBET_SITE_ID)));

                    CrawlRaceData result = new CrawlRaceData();
                    result.setSiteEnum(SiteEnum.ZBET);
                    result.setStatus(ConvertBase.getZBetRaceStatus(zBetRaceRawData.getStatus()));
                    result.setMapEntrants(mapEntrants);

                    if (zBetRaceRawData.getFinalResult() != null) {
                        String raceResult = zBetRaceRawData.getFinalResult().replace('/', ',');
                        Map<Integer, String> mapRaceResult = Collections.singletonMap(AppConstant.ZBET_SITE_ID, raceResult);
                        if (AppConstant.STATUS_FINAL.equals(result.getStatus())) {
                            result.setFinalResult(mapRaceResult);
                        } else if (AppConstant.STATUS_INTERIM.equals(result.getStatus())) {
                            result.setInterimResult(mapRaceResult);
                        }
                    }

                    return result;
                });
    }

    private List<EntrantRawData> getListEntrant(String raceId, ZBetRaceRawData raceDto) {
        Map<Integer, Integer> positionResult = crawUtils.getPositionInResult(raceDto.getFinalResult());

        return raceDto.getSelections().stream().filter(f -> f.getName() != null && f.getNumber() != null)
                .map(m -> EntrantMapper.mapCrawlEntrant(raceId, m, buildPriceFluctuations(m), positionResult)).collect(Collectors.toList());
    }

    private List<MeetingDto> getAllAusMeeting(List<ZBetMeetingRawData> zBetMeeting, LocalDate date) {
        log.info("[ZBET] Sum all meeting: {}", zBetMeeting.size());
        log.info("[ZBET] Sum all race: {}", zBetMeeting.stream().mapToInt(race -> race.getRaces().size()).sum());
        List<ZBetMeetingRawData> ausZBetMeeting = new ArrayList<>(zBetMeeting);

        Map<Meeting, List<Race>> mapMeetingAndRace = new HashMap<>();
        List<ZBetRacesData> racesData = new ArrayList<>();

        ausZBetMeeting.forEach(meeting -> {

            List<ZBetRacesData> meetingRaces = meeting.getRaces();
            meetingRaces.forEach(race -> {
                race.setMeetingName(meeting.getName());
                race.setType(ConvertBase.convertRaceTypeOfTab(race.getType()));
                race.setStatus(ConvertBase.getZBetRaceStatus(race.getStatus()));
            });
            racesData.addAll(meetingRaces);
            mapMeetingAndRace.put(MeetingMapper.toMeetingEntity(meeting), meetingRaces.stream().map(MeetingMapper::toRaceEntity).collect(Collectors.toList()));
        });

        List<RaceDto> raceDtoList = racesData.stream().map(MeetingMapper::toRaceDto).collect(Collectors.toList());

        Mono<Map<String, Long>> mapUUIDToRaceIdMono = crawUtils.saveMeetingSiteAndRaceSite(mapMeetingAndRace, SiteEnum.ZBET.getId());

        setRaceIdThenCrawlAndSaveEntrants(mapUUIDToRaceIdMono, raceDtoList, date);

        return Collections.emptyList();
    }

    @Override
    public Flux<EntrantDto> crawlAndSaveEntrantsInRace(RaceDto raceDto, LocalDate date) {
        String raceUUID = raceDto.getId();

        return getZBetRaceData(raceUUID)
                .doOnError(throwable -> crawUtils.saveFailedCrawlRace(this.getClass().getName(), raceDto, date))
                 .flatMapMany(raceRawData -> {
                    List<ZBetEntrantData> allEntrant = raceRawData.getSelections();
                    List<Entrant> newEntrants = allEntrant.stream().distinct()
                            .map(entrantData -> MeetingMapper.toEntrantEntity(entrantData, buildPriceFluctuations(entrantData))).collect(Collectors.toList());

                    return Mono.justOrEmpty(raceDto.getRaceId())
                            .switchIfEmpty(crawUtils.getIdForNewRaceAndSaveRaceSite(raceDto, newEntrants, SiteEnum.ZBET.getId())
                                                    .doOnNext(raceDto::setRaceId)
                            ) // Get id for new race and save race site if raceDto.getRaceId() == null
                            .flatMapIterable(raceId -> {
                                raceDto.setDistance(raceRawData.getDistance());
                                if (AppConstant.STATUS_FINAL.equals(raceDto.getStatus()) && raceRawData.getFinalResult() != null) {
                                    String finalResult = raceRawData.getFinalResult().replace('/', ',');
                                    crawUtils.updateRaceFinalResultIntoDB(raceDto.getRaceId(), finalResult, AppConstant.ZBET_SITE_ID);
                                    raceDto.setFinalResult(finalResult);
                                }
                                saveEntrant(newEntrants, raceDto);

                                return allEntrant.stream().map(entrantData -> EntrantMapper.toEntrantDto(entrantData, buildPriceFluctuations(entrantData))).collect(Collectors.toList());
                            });
                });
    }

    private void saveEntrant(List<Entrant> newEntrants, RaceDto raceDto) {
        if (raceDto == null){
            return;
        }

        crawUtils.saveEntrantCrawlDataToRedis(newEntrants, raceDto, AppConstant.ZBET_SITE_ID);

        crawUtils.saveEntrantsPriceIntoDB(newEntrants, raceDto.getRaceId(), AppConstant.ZBET_SITE_ID);
    }

    private Mono<ZBetRaceRawData> getZBetRaceData(String raceId) {
        String raceQueryURI = AppConstant.ZBET_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
        return crawUtils.crawlData(zbetsWebClient, raceQueryURI, ZbetRaceResponseRawData.class, this.getClass().getName(), 0L)
                        .mapNotNull(ZbetRaceResponseRawData::getData);
    }

    private List<Float> buildPriceFluctuations(ZBetEntrantData entrantData) {
        if (entrantData != null && entrantData.getPrices() != null) {
            JsonNode priceRawData = entrantData.getPrices();

            Collection<ZBetPrices> zBetPricesCollection;
            if (!priceRawData.isArray()) {
                Map<Integer, ZBetPrices> mapPrices = new ObjectMapper().convertValue(priceRawData, new TypeReference<Map<Integer, ZBetPrices>>() {});
                zBetPricesCollection = mapPrices.values();

            } else {
                zBetPricesCollection = new ObjectMapper().convertValue(priceRawData, new TypeReference<ArrayList<ZBetPrices>>() {});
            }

            if (!CollectionUtils.isEmpty(zBetPricesCollection)) {
                List<ZBetPrices> listZBF = zBetPricesCollection.stream().filter(zBetPrices -> AppConstant.VALID_CHECK_PRODUCT_CODE.equals(zBetPrices.getProductCode()))
                        .sorted(Comparator.comparing(ZBetPrices::getRequestedAt)).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(listZBF)) {
                    return Collections.emptyList();
                }
                List<String> lastFluctuations = Arrays.stream(listZBF.get(listZBF.size() - 1).getFluctuations().split(",")).collect(Collectors.toList());
                return lastFluctuations.stream().map(Float::parseFloat).filter(x -> x != 0).collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}
