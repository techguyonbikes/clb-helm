package com.tvf.clb.base.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tvf.clb.base.dto.RaceResponseDto;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.model.PriceHistoryData;
import io.r2dbc.postgresql.codec.Json;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tvf.clb.base.utils.AppConstant.*;


public class CommonUtils {

    public static Map<Integer, List<PriceHistoryData>> getSitePriceFromJsonb(Json json) {
        if (json == null) {
            return new HashMap<>();
        }
        Type type = new TypeToken<Map<Integer, List<PriceHistoryData>>>() {}.getType();
        return new Gson().fromJson(json.asString(), type);
    }

    // "NSW", "SA", "VIC", "QLD", "NT", "TAS", "WA","NZL"
    public static String checkDiffStateMeeting(String state) {
        if (AppConstant.CODE_NZL.equals(state)) {
            return AppConstant.CODE_NZ ;
        }
        return state;
    }

    public static Map<Integer, String> getMapRaceFinalResultFromJsonb(Json json) {
        if (json == null) {
            return new HashMap<>();
        }
        Type type = new TypeToken<Map<Integer, String>>() {}.getType();
        return new Gson().fromJson(json.asString(), type);
    }

    public static boolean isRaceFinalOrAbandonedInAllSite(RaceResponseDto race) {
        String raceStatus = race.getStatus();

        if (race.getStatus() == null) {
            return false;
        } else if (AppConstant.STATUS_ABANDONED.equals(raceStatus)) {
            return true;
        } else if (AppConstant.STATUS_FINAL.equals(raceStatus) || AppConstant.STATUS_RE_RESULTED.equals(raceStatus)) {
            // check all site have final result
            return race.getFinalResult() != null && race.getFinalResult().size() == race.getMapSiteUUID().size();
        }

        return false;
    }


    public static Race getRaceDiffRaceName(List<Race> races, String raceNameCompare, Instant advertisedStart) {
        if (races.isEmpty() || raceNameCompare == null || advertisedStart == null) {
            return null;
        }
        if (races.size() == 1){
            return races.get(0);
        }
        Race result = races.get(0);
        int wordMax = compareName(races.get(0).getName(), raceNameCompare);
        Map<Race, Integer> mapRaceIdAndWordSame = new HashMap<>();
        for (Race race : races) {
            if (race.getName() == null || race.getAdvertisedStart() == null) {
                continue;
            }
            if (race.getName().equals(raceNameCompare) && race.getAdvertisedStart().equals(advertisedStart)) {
                return race;
            } else {
                int numberOfSameWord = compareName(race.getName(), raceNameCompare);
                if (wordMax < numberOfSameWord) {
                    wordMax = numberOfSameWord;
                    result = race;
                }
                mapRaceIdAndWordSame.put(race, numberOfSameWord);
            }
        }
        Race raceSameWithAdvertisedStart = getRaceSameWithAdvertisedStart(mapRaceIdAndWordSame, wordMax, advertisedStart);
        return raceSameWithAdvertisedStart != null ? raceSameWithAdvertisedStart : result;
    }

    public static Race getRaceSameWithAdvertisedStart(Map<Race, Integer> mapRaceIdAndWordSame, int wordMax, Instant advertisedStart){
        if (mapRaceIdAndWordSame == null || mapRaceIdAndWordSame.isEmpty() || advertisedStart == null){
            return null;
        }
        for (Map.Entry<Race, Integer> entry : mapRaceIdAndWordSame.entrySet()) {
            Race race = entry.getKey();
            Integer word = entry.getValue();
            if (word.equals(wordMax) && race.getAdvertisedStart().equals(advertisedStart)){
                return race;
            }
        }
        return null;
    }

    public static Meeting mapNewMeetingToExisting(Map<Meeting, List<Race>> mapExisingMeetingAndRace, Meeting meetingNeedToCheck, List<Race> racesNeedToCheck) {
        if (mapExisingMeetingAndRace.isEmpty() || meetingNeedToCheck == null){
            return null;
        }

        List<Meeting> mostCommonMeetings = new ArrayList<>();
        int maxCommonWords = 0;

        for (Meeting existing : mapExisingMeetingAndRace.keySet()) {
            int numberOfCommonWord = compareName(existing.getName(), meetingNeedToCheck.getName());

            if (numberOfCommonWord > maxCommonWords) {
                maxCommonWords = numberOfCommonWord;
                mostCommonMeetings = new ArrayList<>();
                mostCommonMeetings.add(existing);
            } else if (numberOfCommonWord > 0 && numberOfCommonWord == maxCommonWords) {
                mostCommonMeetings.add(existing);
            }
        }

        if (mostCommonMeetings.isEmpty()) {
            return null;
        } else if (mostCommonMeetings.size() == 1) {
            return mostCommonMeetings.get(0);
        } else {
            Meeting result = null;
            int maxCommonRace = 0;
            for (Meeting meeting: mostCommonMeetings) {
                int numberOfCommonRaces = compareRaces(mapExisingMeetingAndRace.get(meeting), racesNeedToCheck);
                if (numberOfCommonRaces > maxCommonRace) {
                    maxCommonRace = numberOfCommonRaces;
                    result = meeting;
                }
            }
            return result;
        }
    }

    public static int compareRaces(List<Race> targetRaces, List<Race> listRaceNeedToCheck) {
        int numberOfCommonRaces = 0;
        Map<Integer, Race> mapNumberAndTargetRace = targetRaces.stream().collect(Collectors.toMap(Race::getNumber, Function.identity()));
        Map<Integer, Race> mapNumberAndRaceNeedToCheck = listRaceNeedToCheck.stream().collect(Collectors.toMap(Race::getNumber, Function.identity()));

        for (Map.Entry<Integer, Race> entry : mapNumberAndTargetRace.entrySet()) {
            Integer number = entry.getKey();
            Race targetRace = entry.getValue();
            if (mapNumberAndRaceNeedToCheck.containsKey(number)) {
                Race raceNeedToCheck = mapNumberAndRaceNeedToCheck.get(number);
                if ((targetRace.getName().contains(raceNeedToCheck.getName()) || raceNeedToCheck.getName().contains(targetRace.getName()))
                        && targetRace.getAdvertisedStart().equals(raceNeedToCheck.getAdvertisedStart())) {
                    numberOfCommonRaces++;
                }
            }
        }
        return numberOfCommonRaces;
    }

    public static int compareName(String meetingName1, String meetingName2){
        if (meetingName1 == null || meetingName2 == null) {
            return 0;
        }
        meetingName1 = meetingName1.toLowerCase();
        meetingName2 = meetingName2.toLowerCase();

        String[] words1 = meetingName1.split("\\s+");
        String[] words2 = meetingName2.split("\\s+");

        HashSet<String> uniqueWords = new HashSet<>(Arrays.asList(words2));

        int commonWords = 0;

        for (String word : words1) {
            if (uniqueWords.contains(word)) {
                commonWords++;
            }
        }

        return commonWords;
    }

    public static String getStringInstantDateNow() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .withZone(ZoneId.of("UTC"))
                .format(Instant.now().atZone(ZoneOffset.UTC).toInstant());
    }

    public static List<PriceHistoryData> convertToPriceHistoryData(List<Float> price) {
        if (price == null) {
            return new ArrayList<>();
        }
        return price.stream().map(x -> new PriceHistoryData(x, getStringInstantDateNow())).collect(Collectors.toList());
    }

    public static boolean compareDatesAfter(Instant date1, Instant date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.isAfter(date2);
    }

    public static List<Long> convertStringToListLong(String ids){
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return Arrays.stream(ids.trim().split(",")).map(Long::parseLong).distinct().collect(Collectors.toList());
        }catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static <T> void setIfPresent(T value, Consumer<T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }

    public static Json toJsonb(Object source) {
        return Json.of(new Gson().toJson(source));
    }

    public static <T> T fromJsonbToObject(Json json, TypeToken<T> typeToken) {
        Gson gson = new Gson();
        return gson.fromJson(json.asString(), typeToken.getType());
    }

    private static final Map<String, Integer> statusOrder;
    static {
        statusOrder = new HashMap<>();
        statusOrder.put(STATUS_OPEN, OPEN_STATUS_ORDER);
        statusOrder.put(STATUS_CLOSED, CLOSE_STATUS_ORDER);
        statusOrder.put(STATUS_INTERIM, INTERIM_STATUS_ORDER);
        statusOrder.put(STATUS_FINAL, FINAL_STATUS_ORDER);
        statusOrder.put(STATUS_RE_RESULTED, RE_RESULTED_STATUS_ORDER);
        statusOrder.put(STATUS_SUSPENDED, SUSPENDED_STATUS_ORDER);
        statusOrder.put(STATUS_ABANDONED, ABANDONED_STATUS_ORDER);
    }

    public static Integer getStatusOrder(String status) {
        return statusOrder.get(status);
    }

}
