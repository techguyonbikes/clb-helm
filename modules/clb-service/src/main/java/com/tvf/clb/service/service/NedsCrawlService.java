package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.exception.ApiRequestFailedException;
import com.tvf.clb.base.model.*;
import com.tvf.clb.base.utils.ApiUtils;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.ConvertBase;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ClbService(componentType =  AppConstant.NED)
@Slf4j
public class NedsCrawlService implements ICrawlService{

    @Autowired
    private CrawUtils crawUtils;

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {

        log.info("Start getting the API from Ned.");

        CrawlMeetingFunction crawlFunction = crawDate -> {
            String url = AppConstant.NEDS_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
            Response response = ApiUtils.get(url);
            ResponseBody body = response.body();
            Gson gson = new GsonBuilder().setDateFormat(AppConstant.DATE_TIME_FORMAT_LONG).create();
            if (body != null) {
                LadBrokedItMeetingDto rawData = gson.fromJson(body.string(), LadBrokedItMeetingDto.class);

                return getAllAusMeeting(rawData, date);
            }
            return null;
        };

        return crawUtils.crawlMeeting(crawlFunction, date, 20000L, this.getClass().getName());
    }

    @Override
    public CrawlRaceData getEntrantByRaceUUID(String raceId) {

        LadBrokedItRaceDto raceDto = getNedsRaceDto(raceId);

        if (raceDto == null || raceDto.getRaces() == null) {
            return new CrawlRaceData();
        }

        Map<String, LadbrokesRaceResult> results = raceDto.getResults();

        Map<String, Integer> positions = new HashMap<>();
        if (results != null) {
            positions = results.keySet().stream().collect(Collectors.toMap(Function.identity(), key -> results.get(key).getPosition()));
        } else {
            positions.put(AppConstant.POSITION, 0);
        }
        HashMap<String, ArrayList<Float>> allEntrantPrices = raceDto.getPriceFluctuations();
        List<EntrantRawData> allEntrant = crawUtils.getListEntrant(raceDto, allEntrantPrices, raceId, positions);

        Map<Integer, CrawlEntrantData> mapEntrants = new HashMap<>();
        allEntrant.forEach(x -> {
            List<Float> entrantPrice;
            if (allEntrantPrices == null) {
                entrantPrice = new ArrayList<>();
            } else {
                entrantPrice = allEntrantPrices.get(x.getId()) == null ? new ArrayList<>()
                        : new ArrayList<>(allEntrantPrices.get(x.getId()));
            }
            Map<Integer, List<Float>> priceFluctuations = new HashMap<>();
            priceFluctuations.put(AppConstant.NED_SITE_ID, entrantPrice);
            mapEntrants.put(x.getNumber(), new CrawlEntrantData(x.getPosition(), priceFluctuations));
        });

        CrawlRaceData result = new CrawlRaceData();
        result.setSiteEnum(SiteEnum.NED);
        result.setMapEntrants(mapEntrants);

        Optional<String> optionalStatus = getStatusFromRaceMarket(raceDto.getMarkets());
        if (optionalStatus.isPresent() && optionalStatus.get().equals(AppConstant.STATUS_FINAL)) {
            String top4Entrants = getWinnerEntrants(allEntrant)
                                        .map(entrant -> String.valueOf(entrant.getNumber()))
                                        .collect(Collectors.joining(","));

            result.setFinalResult(Collections.singletonMap(AppConstant.NED_SITE_ID, top4Entrants));
        }

        return result;
    }

    private List<MeetingDto> getAllAusMeeting(LadBrokedItMeetingDto ladBrokedItMeetingDto, LocalDate date) {

        Collection<VenueRawData> venues = ladBrokedItMeetingDto.getVenues().values();
        log.info("[NEDS] Sum all meetings: "+ladBrokedItMeetingDto.getMeetings().values().size());
        log.info("[NEDS] Sum all races: "+ladBrokedItMeetingDto.getRaces().values().size());
        List<String> venuesId = venues.stream().map(VenueRawData::getId).collect(Collectors.toList());

        Map<String, VenueRawData> meetingVenue = venues.stream().collect(Collectors.toMap(VenueRawData::getId, Function.identity()));

        List<MeetingRawData> meetings = ladBrokedItMeetingDto.getMeetings().values().stream()
                .filter(m -> venuesId.contains(m.getVenueId()) && m.getTrackCondition() != null)
                .collect(Collectors.toList());

        meetings.forEach(meeting -> {
            meeting.setState(meetingVenue.get(meeting.getVenueId()).getState());
            meeting.setCountry(meetingVenue.get(meeting.getVenueId()).getCountry());
        });

        List<String> raceIds = meetings.stream().map(MeetingRawData::getRaceIds).flatMap(List::stream).collect(Collectors.toList());

        List<RaceRawData> races = ladBrokedItMeetingDto.getRaces().values().stream().filter(r -> raceIds.contains(r.getId())).collect(Collectors.toList());
        List<MeetingDto> meetingDtoList = new ArrayList<>();

        Map<Meeting, List<Race>> mapMeetingAndRace = new HashMap<>();

        for (MeetingRawData localMeeting : meetings) {
            List<RaceRawData> localRace = races.stream().filter(r -> localMeeting.getRaceIds().contains(r.getId())).collect(Collectors.toList());
            crawUtils.checkMeetingWrongAdvertisedStart(localMeeting, localRace);
            MeetingDto meetingDto = MeetingMapper.toMeetingDto(localMeeting, localRace);
            meetingDtoList.add(meetingDto);
            mapMeetingAndRace.put(MeetingMapper.toMeetingEntity(meetingDto), meetingDto.getRaces().stream().filter(race -> race.getNumber() != null).map(MeetingMapper::toRaceEntityFromNED).collect(Collectors.toList()));
        }
        saveMeetingSiteAndRaceSite(mapMeetingAndRace);

        List<RaceDto> raceDtoList = meetingDtoList.stream().map(MeetingDto::getRaces).flatMap(List::stream).filter(x -> x.getNumber() != null).collect(Collectors.toList());
        crawlAndSaveEntrants(raceDtoList, date).subscribe();
        return meetingDtoList;
    }

    private void saveMeetingSiteAndRaceSite(Map<Meeting, List<Race>> mapMeetingAndRace) {
        crawUtils.saveMeetingSiteAndRaceSite(mapMeetingAndRace, SiteEnum.NED.getId());
    }

    @Override
    public Flux<EntrantDto> crawlAndSaveEntrantsInRace(RaceDto raceDto, LocalDate date) {

        String raceUUID = raceDto.getId();
        LadBrokedItRaceDto raceRawData = getNedsRaceDto(raceUUID);
        if (raceRawData != null) {
            if (raceRawData.getRaces() == null || raceRawData.getMeetings() == null) {
                return Flux.empty();
            }
            Map<String, LadbrokesRaceResult> results = raceRawData.getResults();
            Map<String, Integer> positions = new HashMap<>();
            if (results != null) {
                positions = results.keySet().stream().collect(Collectors.toMap(Function.identity(), key -> results.get(key).getPosition()));
            } else {
                positions.put(AppConstant.POSITION, 0);
            }

            HashMap<String, ArrayList<Float>> allEntrantPrices = raceRawData.getPriceFluctuations();
            List<EntrantRawData> allEntrant = crawUtils.getListEntrant(raceRawData, allEntrantPrices, raceUUID, positions);

            Optional<String> optionalStatus = getStatusFromRaceMarket(raceRawData.getMarkets());
            if (optionalStatus.isPresent() && optionalStatus.get().equals(AppConstant.STATUS_FINAL)) {
                String top4Entrants = getWinnerEntrants(allEntrant)
                        .map(entrant -> String.valueOf(entrant.getNumber()))
                        .collect(Collectors.joining(","));

                raceDto.setFinalResult(top4Entrants);
                crawUtils.updateRaceFinalResultIntoDB(raceDto, AppConstant.NED_SITE_ID, top4Entrants);
            }

            saveEntrant(allEntrant, raceDto, date);
            return Flux.fromIterable(allEntrant)
                    .flatMap(r -> {
                        List<Float> entrantPrices = CollectionUtils.isEmpty(allEntrantPrices) ? new ArrayList<>() : allEntrantPrices.get(r.getId());
                        EntrantDto entrantDto = EntrantMapper.toEntrantDto(r, entrantPrices);
                        return Mono.just(entrantDto);
                    });

        } else  {
            crawUtils.saveFailedCrawlRace(this.getClass().getName(), raceDto, date);
            throw new ApiRequestFailedException();
        }

    }

    private Stream<EntrantRawData> getWinnerEntrants(List<EntrantRawData> entrants) {
        return entrants.stream().parallel()
                .filter(entrant -> entrant.getPosition() > 0)
                .sorted(Comparator.comparing(EntrantRawData::getPosition))
                .limit(4);
    }

    private Optional<String> getStatusFromRaceMarket(Map<String, LadbrokesMarketsRawData> marketsRawDataMap) {
        Optional<LadbrokesMarketsRawData> finalFieldMarket = marketsRawDataMap.values().stream().filter(market -> market.getName().equals(AppConstant.MARKETS_NAME)).findFirst();
        return finalFieldMarket.flatMap(market -> ConvertBase.getLadbrokeRaceStatus(market.getMarketStatusId()));
    }

    public void saveEntrant(List<EntrantRawData> entrantRawData, RaceDto raceDto, LocalDate date) {
        List<Entrant> newEntrants = entrantRawData.stream().distinct().map(MeetingMapper::toEntrantEntity).collect(Collectors.toList());

        String raceIdIdentifierInRedis = String.format("%s - %s - %s - %s", raceDto.getMeetingName(), raceDto.getNumber(), raceDto.getRaceType(), date);
        crawUtils.saveEntrantCrawlDataToRedis(newEntrants, AppConstant.NED_SITE_ID, raceIdIdentifierInRedis, raceDto);

        crawUtils.saveEntrantsPriceIntoDB(newEntrants, raceDto, AppConstant.NED_SITE_ID);
    }

    public LadBrokedItRaceDto getNedsRaceDto(String raceId) {

        CrawlRaceFunction crawlFunction = raceUUID -> {
            String url = AppConstant.NEDS_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
            Response response = ApiUtils.get(url);
            JsonObject jsonObject = JsonParser.parseString(response.body().string()).getAsJsonObject();
            Gson gson = new GsonBuilder().create();
            return gson.fromJson(jsonObject.get("data"), LadBrokedItRaceDto.class);
        };

        Object result = crawUtils.crawlRace(crawlFunction, raceId, this.getClass().getName());

        if (result == null) {
            return null;
        }
        return (LadBrokedItRaceDto) result;
    }
}
