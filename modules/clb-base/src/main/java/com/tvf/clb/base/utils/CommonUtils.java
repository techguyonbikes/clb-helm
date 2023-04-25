package com.tvf.clb.base.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tvf.clb.base.dto.RaceResponseDto;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import io.r2dbc.postgresql.codec.Json;

import java.lang.reflect.Type;
import java.util.*;


public class CommonUtils {

    public static Map<Integer, List<Float>> getSitePriceFromJsonb(Json json) {
        if (json == null) {
            return new HashMap<>();
        }
        Type type = new TypeToken<Map<Integer, List<Float>>>() {}.getType();
        return new Gson().fromJson(json.asString(), type);
    }

    // "NSW", "SA", "VIC", "QLD", "NT", "TAS", "WA","NZL"
    public static String checkDiffStateMeeting(String state) {
        if (AppConstant.VALID_CHECK_CODE_STATE_DIFF.contains(state)) {
            return AppConstant.CODE_NZL.equals(state) ? AppConstant.CODE_NZ : state;
        }
        return null;
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
        } else if (AppConstant.STATUS_FINAL.equals(raceStatus)) {
            return race.getFinalResult().size() == race.getMapSiteUUID().size(); // check all site have final result
        }

        return false;
    }


    public static Race checkDiffRaceName(List<Race> races, String raceNameCompare) {
        if (races.isEmpty() || raceNameCompare == null) {
            return null;
        }
        if (races.size() == 1){
            return races.get(0);
        }
        Race result = races.get(0);
        int wordMax = compareName(races.get(0).getName(), raceNameCompare);
        for (Race race : races) {
            if (race.getName() != null) {
                if (race.getName().equals(raceNameCompare)) {
                    return race;
                } else {
                    int numberOfSameWord = compareName(race.getName(), raceNameCompare);
                    if (wordMax < numberOfSameWord) {
                        wordMax = numberOfSameWord;
                        result = race;
                    }
                }
            }
        }
        return result;
    }

    public static Meeting checkDiffMeetingName(List<Meeting> meetings, String meetingNameCompare) {
        if (meetings.isEmpty() || meetingNameCompare == null){
            return null;
        }
        if (meetings.size() == 1){
            return meetings.get(0);
        }
        Meeting result = meetings.get(0);
        int wordMax = compareName(meetings.get(0).getName(), meetingNameCompare);
        for (Meeting meeting : meetings) {
            String name = meeting.getName();
            if (name != null) {
                if (name.equals(meetingNameCompare)) {
                    return meeting;
                } else {
                    int numberOfSameWord = compareName(name, meetingNameCompare);
                    if (wordMax < numberOfSameWord) {
                        result = meeting;
                        wordMax = numberOfSameWord;
                    }
                }
            }
        }
        return result;
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

}
