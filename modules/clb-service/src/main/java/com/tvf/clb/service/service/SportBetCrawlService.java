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
import com.tvf.clb.base.model.sportbet.SportBetMeetingRawData;
import com.tvf.clb.base.utils.ApiUtils;
import com.tvf.clb.base.utils.AppConstant;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tvf.clb.base.utils.AppConstant.SPORT_BET_BETTING_STATUS_RESULTED;

@ClbService(componentType = AppConstant.SPORT_BET)
@Slf4j
public class SportBetCrawlService implements ICrawlService{

    @Autowired
    private CrawUtils crawUtils;

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {
        return Mono.fromSupplier(() -> {
            SportBetDataDto rawData = null;
            try {
                Thread.sleep(20000);
                String url = AppConstant.SPORT_BET_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
                Response response = ApiUtils.get(url);
                ResponseBody body = response.body();
                Gson gson = new GsonBuilder().setDateFormat(AppConstant.DATE_TIME_FORMAT_LONG).create();
                if (body != null) {
                    rawData = gson.fromJson(body.string(), SportBetDataDto.class);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log.info("Start getting the API from SportBet.");
            return getAllAusMeeting(rawData,date);
        }).flatMapMany(Flux::fromIterable);
    }
    private List<MeetingDto> getAllAusMeeting(SportBetDataDto sportBetDataDto, LocalDate date) {
        List<SportBetSectionsDto> sportBetSectionsDtos = sportBetDataDto.getDates();
        List<SportBetMeetingDto> sportBetMeetingDtoList = sportBetSectionsDtos.get(0).getSections();
        List<MeetingDto> meetingDtoList = new ArrayList<>();
        for(SportBetMeetingDto meetingDto :sportBetMeetingDtoList){
            List<SportBetMeetingRawData> meetingRawData = meetingDto.getMeetings().stream().filter(r->AppConstant.VALID_COUNTRY_SPORT_BET.contains(r.getRegionName())).collect(Collectors.toList());
            meetingDtoList.addAll(MeetingMapper.toMeetingSportDtoList(meetingDto,meetingRawData,date));
        }
        saveMeeting(meetingDtoList);
        List<RaceDto> raceDtoList = new ArrayList<>();
        meetingDtoList.forEach(meeting -> {
            List<RaceDto> meetingRaces = meeting.getRaces();
            meetingRaces.forEach(race -> {
                race.setMeetingName(meeting.getName());
                race.setRaceType(meeting.getRaceType());
            });
            raceDtoList.addAll(meetingRaces);
        });
        saveRace(raceDtoList);
        crawlAndSaveAllEntrants(raceDtoList, date).subscribe();
        return Collections.emptyList();
    }
    public void saveMeeting(List<MeetingDto> meetingDtoList) {
        List<Meeting> newMeetings = meetingDtoList.stream().map(MeetingMapper::toMeetingEntity).collect(Collectors.toList());
        crawUtils.saveMeetingSite(newMeetings, AppConstant.SPORTBET_SITE_ID);
    }

    public void saveRace(List<RaceDto> raceDtoList) {
        List<Race> newRaces = raceDtoList.stream().map(MeetingMapper::toRaceEntity).collect(Collectors.toList());
        crawUtils.saveRaceSite(newRaces, AppConstant.SPORTBET_SITE_ID);
    }

    @Override
    public CrawlRaceData getEntrantByRaceUUID(String raceId) {
        try {
            SportBetRaceDto sportBetRaceDto = crawlEntrantDataSportBet(raceId);
            MarketRawData  markets = sportBetRaceDto.getMarkets().get(0);
            List<SportBetEntrantRawData> allEntrant = markets.getSelections();

            Map<Integer, CrawlEntrantData> entrantMap = new HashMap<>();
            allEntrant.forEach(x -> {
                List<Float> prices = new ArrayList<>();
                Map<Integer, List<Float>> priceFluctuations = new HashMap<>();
                prices.add(x.getStatistics().getOpenPrice());
                prices.add(x.getStatistics().getFluc1());
                prices.add(x.getStatistics().getFluc2());
                x.getPrices().stream().filter(r->AppConstant.PRICE_CODE.equals(r.getPriceCode())).findFirst().ifPresent(
                       r->prices.add(r.getWinPrice())
                );
                priceFluctuations.put(AppConstant.SPORTBET_SITE_ID, prices);
                entrantMap.put(x.getRunnerNumber(), new CrawlEntrantData(0, priceFluctuations));
            });

            CrawlRaceData result = new CrawlRaceData();
            result.setSiteId(SiteEnum.SPORT_BET.getId());
            result.setMapEntrants(entrantMap);

            if (isRaceStatusFinal(sportBetRaceDto)) {
                String top4Entrants = getWinnerEntrants(sportBetRaceDto.getResults())
                        .map(resultsRawData -> resultsRawData.getRunnerNumber().toString())
                        .collect(Collectors.joining(","));

                result.setFinalResult(Collections.singletonMap(AppConstant.SPORTBET_SITE_ID, top4Entrants));
            }

            return result;
        } catch (IOException e) {
            throw new ApiRequestFailedException("API request failed: " + e.getMessage(), e);
        }
    }

    public Flux<EntrantDto> crawlAndSaveAllEntrants(List<RaceDto> raceDtoList, LocalDate date) {
        return Flux.fromIterable(raceDtoList)
                .parallel()
                .runOn(Schedulers.parallel())
                .flatMap(raceDto -> crawlAndSaveEntrantsInRace(raceDto, date))
                .sequential();
    }

    public Flux<EntrantDto> crawlAndSaveEntrantsInRace(RaceDto raceDto, LocalDate date) {
        try {
            String raceUUID = raceDto.getId();
            SportBetRaceDto sportBetRaceDto = crawlEntrantDataSportBet(raceUUID);
            MarketRawData  markets = sportBetRaceDto.getMarkets().get(0);
            if (markets != null) {
                List<SportBetEntrantRawData> allEntrant = markets.getSelections();
                String raceIdIdentifierInRedis = String.format("%s - %s - %s - %s", raceDto.getMeetingName(), raceDto.getNumber(), raceDto.getRaceType(), date);
                saveEntrant(allEntrant, raceIdIdentifierInRedis, raceDto);
            } else {
                log.error("Can not found SportBet race by RaceUUID " + raceUUID);
            }

            if (isRaceStatusFinal(sportBetRaceDto)) {
                String top4Entrants = getWinnerEntrants(sportBetRaceDto.getResults())
                                            .map(resultsRawData -> resultsRawData.getRunnerNumber().toString())
                                            .collect(Collectors.joining(","));

                crawUtils.updateRaceFinalResultIntoDB(raceDto, AppConstant.SPORTBET_SITE_ID, top4Entrants);
            }

            return Flux.empty();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isRaceStatusFinal(SportBetRaceDto sportBetRaceDto) {
        return ! CollectionUtils.isEmpty(sportBetRaceDto.getResults())
                && sportBetRaceDto.getBettingStatus().equals(SPORT_BET_BETTING_STATUS_RESULTED);
    }

    private Stream<ResultsRawData> getWinnerEntrants(List<ResultsRawData> result) {
        return result.stream()
                .sorted(Comparator.comparing(ResultsRawData::getPlace));
    }

    public void saveEntrant(List<SportBetEntrantRawData> entrantRawData, String raceName, RaceDto raceDto) {
        List<Entrant> newEntrants = new ArrayList<>();
        for(SportBetEntrantRawData rawData :entrantRawData){
            List<Float> prices = new ArrayList<>();
            prices.add(rawData.getStatistics().getOpenPrice());
            prices.add(rawData.getStatistics().getFluc1());
            prices.add(rawData.getStatistics().getFluc2());
            rawData.getPrices().stream().filter(r->AppConstant.PRICE_CODE.equals(r.getPriceCode())).findFirst().ifPresent(
                    x->prices.add(x.getWinPrice())
            );
            Entrant entrant = MeetingMapper.toEntrantEntity(rawData,prices);
            newEntrants.add(entrant);
        }
        crawUtils.saveEntrantIntoRedis(newEntrants, AppConstant.SPORTBET_SITE_ID, raceName, raceDto.getId(),null,
                raceDto.getAdvertisedStart(), raceDto.getNumber(), raceDto.getRaceType(), raceDto.getDistance());
    }

    public SportBetRaceDto crawlEntrantDataSportBet(String raceId) throws IOException {
        String url = AppConstant.SPORT_BET_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
        Response response = ApiUtils.get(url);
        JsonObject jsonObject = JsonParser.parseString(response.body().string()).getAsJsonObject();
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(jsonObject.get(AppConstant.RACECARD_EVENT), SportBetRaceDto.class);
    }

}
