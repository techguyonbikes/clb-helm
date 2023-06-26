package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.dto.sportbet.SportBetDataDto;
import com.tvf.clb.base.dto.sportbet.SportBetMeetingDto;
import com.tvf.clb.base.dto.sportbet.SportBetRaceDto;
import com.tvf.clb.base.dto.sportbet.SportBetSectionsDto;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.exception.ApiRequestFailedException;
import com.tvf.clb.base.model.CrawlEntrantData;
import com.tvf.clb.base.model.CrawlRaceData;
import com.tvf.clb.base.model.sportbet.MarketRawData;
import com.tvf.clb.base.model.sportbet.ResultsRawData;
import com.tvf.clb.base.model.sportbet.SportBetEntrantRawData;
import com.tvf.clb.base.model.sportbet.StatisticsRawData;
import com.tvf.clb.base.utils.ApiUtils;
import com.tvf.clb.base.utils.AppConstant;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
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

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {
        log.info("Start getting the API from SportBet.");

        CrawlMeetingFunction crawlFunction = crawlDate -> {
            String url = AppConstant.SPORT_BET_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
            Response response = ApiUtils.get(url);
            ResponseBody body = response.body();
            Gson gson = new GsonBuilder().setDateFormat(AppConstant.DATE_TIME_FORMAT_LONG).create();
            if (body != null) {
                SportBetDataDto rawData = gson.fromJson(body.string(), SportBetDataDto.class);
                return getAllAusMeeting(rawData,date);
            }

            return null;
        };

        return crawUtils.crawlMeeting(crawlFunction, date, 20000L, this.getClass().getName());
    }

    private List<MeetingDto> getAllAusMeeting(SportBetDataDto sportBetDataDto, LocalDate date) {
        List<SportBetSectionsDto> sportBetSectionsDtos = sportBetDataDto.getDates();
        List<SportBetMeetingDto> sportBetMeetingDtoList = sportBetSectionsDtos.get(0).getSections();
        log.info("[SportBet] Sum all meetings:"+sportBetMeetingDtoList.stream().mapToInt(meeting -> meeting.getMeetings().size()).sum());
        log.info("[SportBet] Sum all meetings:"+sportBetMeetingDtoList.stream().mapToInt(meeting -> meeting.getMeetings().stream().mapToInt(race -> race.getEvents().size()).sum()).sum());
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

        return Collections.emptyList();
    }

    @Override
    public CrawlRaceData getEntrantByRaceUUID(String raceId) {
        SportBetRaceDto sportBetRaceDto = crawlEntrantDataSportBet(raceId);

        if (sportBetRaceDto == null) {
            return new CrawlRaceData();
        }

        MarketRawData  markets = sportBetRaceDto.getMarkets().get(0);
        List<SportBetEntrantRawData> allEntrant = markets.getSelections();

        Map<Integer, CrawlEntrantData> entrantMap = new HashMap<>();
        allEntrant.forEach(x -> {

            List<Float> prices = getPricesFromEntrantStatistics(x.getStatistics());
            Map<Integer, List<Float>> priceFluctuations = new HashMap<>();

            x.getPrices().stream().filter(r->AppConstant.PRICE_CODE.equals(r.getPriceCode())).findFirst().ifPresent(
                   r->prices.add(r.getWinPrice())
            );
            priceFluctuations.put(AppConstant.SPORTBET_SITE_ID, prices);
            entrantMap.put(x.getRunnerNumber(), new CrawlEntrantData(0, priceFluctuations));
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
    }

    @Override
    public Flux<EntrantDto> crawlAndSaveEntrantsInRace(RaceDto raceDto, LocalDate date) {
        String raceUUID = raceDto.getId();
        SportBetRaceDto sportBetRaceDto = crawlEntrantDataSportBet(raceUUID);

        if (sportBetRaceDto != null) {
            MarketRawData  markets = sportBetRaceDto.getMarkets().get(0);

            if (isRaceStatusFinal(sportBetRaceDto)) {
                String top4Entrants = getWinnerEntrants(sportBetRaceDto.getResults())
                        .limit(4)
                        .map(resultsRawData -> resultsRawData.getRunnerNumber().toString())
                        .collect(Collectors.joining(","));
                raceDto.setFinalResult(top4Entrants);
                crawUtils.updateRaceFinalResultIntoDB(raceDto.getRaceId(), top4Entrants, AppConstant.SPORTBET_SITE_ID);
            }

            if (markets != null) {
                List<SportBetEntrantRawData> allEntrant = markets.getSelections();
                saveEntrant(allEntrant, raceDto);
            } else {
                log.error("Can not found SportBet race by RaceUUID " + raceUUID);
            }

            return Flux.empty();

        } else {
            crawUtils.saveFailedCrawlRace(this.getClass().getName(), raceDto, date);
            throw new ApiRequestFailedException();
        }
    }

    private boolean isRaceStatusFinal(SportBetRaceDto sportBetRaceDto) {
        return ! CollectionUtils.isEmpty(sportBetRaceDto.getResults())
                && SPORT_BET_BETTING_STATUS_RESULTED.equals(sportBetRaceDto.getBettingStatus());
    }

    private Stream<ResultsRawData> getWinnerEntrants(List<ResultsRawData> result) {
        return result.stream().filter(resultsRawData -> resultsRawData.getPlace() != null)
                .sorted(Comparator.comparing(ResultsRawData::getPlace));
    }

    public void saveEntrant(List<SportBetEntrantRawData> entrantRawData, RaceDto raceDto) {
        List<Entrant> newEntrants = new ArrayList<>();
        for(SportBetEntrantRawData rawData :entrantRawData){
            List<Float> prices = getPricesFromEntrantStatistics(rawData.getStatistics());
            rawData.getPrices().stream().filter(r -> AppConstant.PRICE_CODE.equals(r.getPriceCode())).findFirst().ifPresent(
                    x -> {
                        if (x.getWinPrice() != null) {
                            prices.add(x.getWinPrice());
                        }
                    }
            );
            Entrant entrant = MeetingMapper.toEntrantEntity(rawData,prices);
            newEntrants.add(entrant);
        }
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

    public SportBetRaceDto crawlEntrantDataSportBet(String raceId) {

        CrawlRaceFunction crawlFunction = raceUUID -> {
            String url = AppConstant.SPORT_BET_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
            Response response = ApiUtils.get(url);
            JsonObject jsonObject = JsonParser.parseString(response.body().string()).getAsJsonObject();
            Gson gson = new GsonBuilder().create();
            return gson.fromJson(jsonObject.get(AppConstant.RACECARD_EVENT), SportBetRaceDto.class);
        };

        Object result = crawUtils.crawlRace(crawlFunction, raceId, this.getClass().getName());

        if (result == null) {
            return null;
        }
        return (SportBetRaceDto) result;
    }

}
