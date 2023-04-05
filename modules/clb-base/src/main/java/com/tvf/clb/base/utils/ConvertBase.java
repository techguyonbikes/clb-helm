package com.tvf.clb.base.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.*;
import java.time.format.DateTimeFormatter;

@Slf4j
public class ConvertBase {
    public static String convertRaceTypeOfTab(String feedId) {
        String type = null;
        switch (feedId) {
            case "G":
                type = AppConstant.GREYHOUND_RACING;
                break;
            case "R":
                type = AppConstant.HORSE_RACING;
                break;
            case "H":
                type = AppConstant.HARNESS_RACING;
                break;

        }
        return type;
    }
    public static Instant dateFormat(String dateString){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date = LocalDate.parse(dateString, formatter);
        LocalDateTime startOfDay = date.atStartOfDay();
        return startOfDay.toInstant(ZoneOffset.UTC);
    }

    public static String convertRaceTypeOfSportBet(String raceType) {
        if (AppConstant.GREYHOUND_RACE_TYPE.contains(raceType)) {
            return AppConstant.GREYHOUND_RACING;
        } else if (AppConstant.HORSE_RACE_TYPE.contains(raceType)) {
            return AppConstant.HORSE_RACING;
        } else if (AppConstant.HARNESS_RACE_TYPE.contains(raceType)) {
            return AppConstant.HARNESS_RACING;
        } else {
            return null;
        }
    }
    public static Instant dateFormat(LocalDate date){
        LocalDateTime startOfDay = date.atStartOfDay();
        return startOfDay.toInstant(ZoneOffset.UTC);
    }
}
