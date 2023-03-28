package com.tvf.clb.service.service;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.entity.*;
import com.tvf.clb.base.model.zbet.*;
import com.tvf.clb.base.utils.ApiUtils;
import com.tvf.clb.base.utils.AppConstant;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@ClbService(componentType =  AppConstant.ZBET)
@Slf4j
public class ZBetCrawlService implements ICrawlService {

    @Autowired
    private CrawUtils crawUtils;

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {
        return Mono.fromSupplier(() -> {
            List<ZBetMeetingRawData> rawData = null;
            try {
                Thread.sleep(20000);
                String url = AppConstant.ZBET_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
                Response response = ApiUtils.get(url);
                ResponseBody body = response.body();
                JsonObject jsonObject = JsonParser.parseString(response.body().string()).getAsJsonObject();
                Gson gson = new GsonBuilder().create();
                if (body != null) rawData = gson.fromJson(jsonObject.get("data"), new TypeToken<List<ZBetMeetingRawData>>(){}.getType());
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log.info("Start getting the API from ZBet.");
            return getAllAusMeeting(rawData);
        }).flatMapMany(Flux::fromIterable);
    }

    @Override
    public Map<String, Map<Integer, List<Float>>> getEntrantByRaceId(String raceId) {
        return null;
    }

    private List<MeetingDto> getAllAusMeeting(List<ZBetMeetingRawData> zBetMeeting) {
        List<ZBetMeetingRawData> ausZBetMeeting = zBetMeeting.stream().filter(zBetMeetingRaw ->
                AppConstant.VALID_COUNTRY_CODE.contains(zBetMeetingRaw.getCountry())).collect(Collectors.toList());
        saveMeetingSite(ausZBetMeeting);

        List<ZBetRacesData> racesData = new ArrayList<>();
        ausZBetMeeting.stream().forEach( meeting -> racesData.addAll(meeting.getRaces()));
        saveRaceSite(racesData);

        getEntrantRaceByIds(racesData).subscribe();
        return Collections.emptyList();
    }

    private Flux<EntrantDto> getEntrantRaceByIds(List<ZBetRacesData> raceDtoList) {
        return Flux.fromIterable(raceDtoList)
                .parallel() // create a parallel flux
                .runOn(Schedulers.parallel()) // specify which scheduler to use for the parallel execution
                .flatMap(x -> getEntrantByRaceId(x.getId(), x.getNumber(), x.getName())) // call the getRaceById method for each raceId
                .sequential(); // convert back to a sequential flux
    }

    public Flux<EntrantDto> getEntrantByRaceId(Long raceId, Integer number, String name) {
        ZBetRaceRawData raceDto = getZBetRaceData(raceId.toString());
        if( raceDto != null ) {
            List<ZBetEntrantData> allEntrant = raceDto.getSelections();
            if (raceDto.getDeductions() != null) {
                List<Long> scratched = raceDto.getDeductions().stream().map(Deductions::getSelectionsId).collect(Collectors.toList());
                allEntrant = allEntrant.stream().filter(selection -> !scratched.contains(selection.getId())).collect(Collectors.toList());
            }

            saveEntrant(allEntrant, name, number);
        } else {
            log.info("Can not found ZBet race by RaceId " + raceId);
        }

        return null;
    }

    public void saveMeetingSite(List<ZBetMeetingRawData> meetingRawData) {
        List<Meeting> newMeetings = meetingRawData.stream().map(MeetingMapper::toMeetingEntity).collect(Collectors.toList());
        crawUtils.saveMeetingSite(newMeetings, 3);
    }

    public void saveRaceSite(List<ZBetRacesData> raceDtoList) {
        List<Race> newRaces = raceDtoList.stream().map(MeetingMapper::toRaceEntity).collect(Collectors.toList());
        crawUtils.saveRaceSite(newRaces, 3);
    }

    public void saveEntrant(List<ZBetEntrantData> entrantRawData, String raceName, Integer number) {
        List<Entrant> newEntrants = entrantRawData.stream().distinct()
                //Filter scratched: Prices has type Array
                .filter(xbet ->  xbet.getScratchingTime() == null)
                .map(meeting -> MeetingMapper.toEntrantEntity(meeting, buildPriceFluctuations(meeting))).collect(Collectors.toList());
        crawUtils.saveEntrantIntoRedis(newEntrants, 3, raceName);
    }

    private ZBetRaceRawData getZBetRaceData(String raceId) {
        try {
            String url = AppConstant.ZBET_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
            Response response = ApiUtils.get(url);
            JsonObject jsonObject = JsonParser.parseString(response.body().string()).getAsJsonObject();
            Gson gson = new GsonBuilder().create();
            return gson.fromJson(jsonObject.get("data"), ZBetRaceRawData.class);
        } catch (IOException e) {
            log.error("Got error while get ZBet Race Data raceId: " + raceId);
        }
        return null;
    }

    private List<Float> buildPriceFluctuations(ZBetEntrantData entrantData) {
        if(entrantData.getPrices() instanceof JsonObject){
            Map<Integer,ZBetPrices> pricesMap = new Gson().fromJson(entrantData.getPrices(), new TypeToken<Map<Integer, ZBetPrices>>(){}.getType());
            if (!pricesMap.isEmpty()) {
                List<ZBetPrices> listZBF = pricesMap.values().stream().filter(zBetPrices -> zBetPrices.getProductCode().equals("ZBF"))
                        .sorted(Comparator.comparing(ZBetPrices::getRequestedAt)).collect(Collectors.toList());
                List<String> lastFluctuations = Arrays.stream(listZBF.get(listZBF.size() -1).getFluctuations().split(",")).collect(Collectors.toList());
                return lastFluctuations.stream().map(Float::parseFloat).collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}
