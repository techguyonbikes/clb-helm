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

    public static String getRaceStatusById(Integer tradingStatus, Integer resultStatus) {
        switch (tradingStatus) {
            case 1:
                return AppConstant.STATUS_OPEN;
            case 2:
                return AppConstant.STATUS_SUSPENDED;
            case 3:
                return AppConstant.STATUS_CLOSED;
        }
        switch (resultStatus) {
            case 1:
                return AppConstant.STATUS_INTERIM;
            case 2:
                return AppConstant.STATUS_FINAL;
            case 4:
                return AppConstant.STATUS_ABANDONED;
        }
        return AppConstant.STATUS_OPEN;
    }

}
