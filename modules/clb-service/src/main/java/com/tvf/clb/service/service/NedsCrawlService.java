package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tvf.clb.base.LadbrokesDividendStatus;
import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.exception.ApiRequestFailedException;
import com.tvf.clb.base.model.*;
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

        if (raceDto == null) {
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
        result.setSiteId(SiteEnum.NED.getId());
        result.setMapEntrants(mapEntrants);

        if (isRaceCompleted(results, raceDto.getRaces().get(raceId).getDividends())) {
            String top4Entrants = getWinnerEntrants(allEntrant)
                                        .map(entrant -> String.valueOf(entrant.getNumber()))
                                        .collect(Collectors.joining(","));

            result.setFinalResult(Collections.singletonMap(AppConstant.NED_SITE_ID, top4Entrants));
        }

        return result;
    }

    private List<MeetingDto> getAllAusMeeting(LadBrokedItMeetingDto ladBrokedItMeetingDto, LocalDate date) {
        List<VenueRawData> ausVenues = ladBrokedItMeetingDto.getVenues().values().stream().filter(v -> AppConstant.VALID_COUNTRY_CODE.contains(v.getCountry())).collect(Collectors.toList());
        List<String> venuesId = ausVenues.stream().map(VenueRawData::getId).collect(Collectors.toList());

        Map<String, String> meetingState = ausVenues.stream().collect(Collectors.toMap(VenueRawData::getId, VenueRawData::getState));

        List<MeetingRawData> meetings = new ArrayList<>(ladBrokedItMeetingDto.getMeetings().values());
        List<MeetingRawData> ausMeetings = meetings.stream().filter(m -> venuesId.contains(m.getVenueId()) && m.getTrackCondition() != null)
                .peek(x -> x.setState(meetingState.get(x.getVenueId()))).collect(Collectors.toList());
        List<String> raceIds = ausMeetings.stream().map(MeetingRawData::getRaceIds).flatMap(List::stream)
                .collect(Collectors.toList());
        List<RaceRawData> ausRace = ladBrokedItMeetingDto.getRaces()
                .values().stream().filter(r -> raceIds.contains(r.getId())).collect(Collectors.toList());
        List<MeetingDto> meetingDtoList = new ArrayList<>();
        for (MeetingRawData localMeeting : ausMeetings) {
            List<RaceRawData> localRace = ausRace.stream().filter(r -> localMeeting.getRaceIds().contains(r.getId())).collect(Collectors.toList());
            MeetingDto meetingDto = MeetingMapper.toMeetingDto(localMeeting, localRace);
            meetingDtoList.add(meetingDto);
        }
        saveMeeting(ausMeetings);
        List<RaceDto> raceDtoList = meetingDtoList.stream().map(MeetingDto::getRaces).flatMap(List::stream).filter(x -> x.getNumber() != null).collect(Collectors.toList());
        saveRace(raceDtoList);
        crawlAndSaveEntrants(raceDtoList,  date).subscribe();
        return meetingDtoList;
    }

    @Override
    public Flux<EntrantDto> crawlAndSaveEntrantsInRace(RaceDto raceDto, LocalDate date) {

        String raceUUID = raceDto.getId();
        LadBrokedItRaceDto raceRawData = getNedsRaceDto(raceUUID);
        if (raceRawData != null) {
            Map<String, LadbrokesRaceResult> results = raceRawData.getResults();
            Map<String, Integer> positions = new HashMap<>();
            if (results != null) {
                positions = results.keySet().stream().collect(Collectors.toMap(Function.identity(), key -> results.get(key).getPosition()));
            } else {
                positions.put(AppConstant.POSITION, 0);
            }

            raceDto.setDistance(Integer.valueOf(raceRawData.getRaces().get(raceUUID).getAdditionalInfo().get(AppConstant.DISTANCE).getAsString()));

            HashMap<String, ArrayList<Float>> allEntrantPrices = raceRawData.getPriceFluctuations();
            List<EntrantRawData> allEntrant = crawUtils.getListEntrant(raceRawData, allEntrantPrices, raceUUID, positions);

            if (isRaceCompleted(results, raceRawData.getRaces().get(raceUUID).getDividends())) {
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

    private boolean isRaceCompleted(Map<String, LadbrokesRaceResult> results, List<LadbrokesRaceDividend> dividends) {
        if (results == null || CollectionUtils.isEmpty(dividends)) {
            return false;
        }

        List<LadbrokesDividendStatus> dividendsStatus = dividends.stream().map(LadbrokesRaceDividend::getStatus).collect(Collectors.toList());

        return results.values().stream()
                .map(LadbrokesRaceResult::getResultStatusId)
                .allMatch(statusId -> dividendsStatus.stream().anyMatch(status -> status.getId().equals(statusId)
                        && (status.getName().equalsIgnoreCase(AppConstant.STATUS_FINAL) || status.getName().equalsIgnoreCase(AppConstant.STATUS_INTERIM))));
    }

    public void saveMeeting(List<MeetingRawData> meetingRawData) {
        List<Meeting> newMeetings = meetingRawData.stream().map(MeetingMapper::toMeetingEntity).collect(Collectors.toList());
        crawUtils.saveMeetingSite(newMeetings, AppConstant.NED_SITE_ID);
    }

    public void saveRace(List<RaceDto> raceDtoList) {
        List<Race> newRaces = raceDtoList.stream().map(MeetingMapper::toRaceEntityFromNED).collect(Collectors.toList());
        crawUtils.saveRaceSite(newRaces, AppConstant.NED_SITE_ID);
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
