package com.tvf.clb.base.utils;

import com.tvf.clb.base.dto.RaceBaseResponseDTO;
import com.tvf.clb.base.dto.RaceDto;
import com.tvf.clb.base.model.sportbet.SportBetRacesData;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@NoArgsConstructor(access= AccessLevel.PRIVATE)
public class ConvertBase {
    @Nullable
    @Contract(pure = true)
    public static String convertRaceTypeOfTab(String feedId) {
        switch (feedId) {
            case "G":
                return AppConstant.GREYHOUND_RACING;
            case "R":
                return AppConstant.HORSE_RACING;
            case "H":
                return AppConstant.HARNESS_RACING;
            default:
                return null;
        }

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
            default:
        }
        switch (resultStatus) {
            case 1:
                return AppConstant.STATUS_INTERIM;
            case 2:
                return AppConstant.STATUS_FINAL;
            case 4:
                return AppConstant.STATUS_ABANDONED;
            default:
                return AppConstant.STATUS_OPEN;
        }
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
    public static String getZBetRaceStatus(String status) {
        switch (status) {
            case AppConstant.ZBET_SELLING_STATUS:
                return AppConstant.STATUS_OPEN;
            case AppConstant.ZBET_PAID_STATUS:
            case AppConstant.ZBET_PAYING_STATUS:
                return AppConstant.STATUS_FINAL;
            case AppConstant.ZBET_ABANDONED_STATUS:
                return AppConstant.STATUS_ABANDONED;
            case AppConstant.ZBET_INTERIM_STATUS:
                return AppConstant.STATUS_INTERIM;
            case AppConstant.ZBET_CLOSED_STATUS:
                return AppConstant.STATUS_CLOSED;
            default: return null;
        }
    }
    public static String getDateOfWeek(LocalDate date) {
        String resulst = null;
        LocalDate nowDate = LocalDate.now();
        Integer compareDate =  date.compareTo(nowDate);
        if(compareDate == 0){
            resulst = AppConstant.TODAY;
        }
        if (compareDate > 0 && compareDate < 2){
            resulst = AppConstant.TOMORROW;
        }
        if (compareDate < 0){
            resulst = AppConstant.YESTERDAY;
        }
        if(compareDate >=2){
            DayOfWeek day = date.getDayOfWeek();
            resulst = day.getDisplayName(TextStyle.FULL, Locale.getDefault()) ;
        }
        return resulst;
    }
    public static Instant dateFormatFromString(String date){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, h:mm a");
        LocalDateTime dateTime = LocalDateTime.parse(date, formatter);
        return  dateTime.toInstant(ZoneOffset.UTC).minus(10, ChronoUnit.HOURS);
    }

    public static Instant scratchedTimeFormatFromString(String date){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM/yy");
        LocalDateTime dateTime = LocalDateTime.parse(date, formatter);
        return dateTime.toInstant(ZoneOffset.UTC).minus(10, ChronoUnit.HOURS);
    }
    public static String convertRaceTypeOfTOP(String raceType) {
        switch (raceType) {
            case "GREYHOUNDS":
                return AppConstant.GREYHOUND_RACING;
            case "THOROUGHBREDS":
                return AppConstant.HORSE_RACING;
            case "HARNESS":
                return AppConstant.HARNESS_RACING;
            default: return null;
        }
    }
    public static String getURLRaceOfLadbrokes(String meetingName, String id){
        if(meetingName.contains(" ")){
            meetingName = meetingName.replace(" ","-");
        }
        String url = meetingName.toLowerCase() + "/" + id;
        return AppConstant.URL_LAD_BROKES_IT_RACE.replace(AppConstant.ID_PARAM, url);
    }
    public static String getURLRaceOfNEDS(RaceDto raceDto){
        String meetingName = raceDto.getMeetingName().toLowerCase();
        if(meetingName.contains(" ")){
            meetingName=meetingName.replace("","-");
        }
        String url = meetingName + "/" + raceDto.getId();
        return AppConstant.URL_NEDS_RACE.replace(AppConstant.ID_PARAM, url);
    }
    public static String getURLRaceOfTAP(String raceId, String meetingName){
        if(meetingName.contains(" ")){
            meetingName = meetingName.replace(" ","-");
        }
        //2023-04-27/meetings/H/PEN/races/2
        List<String> a = Arrays.asList(raceId.split("/"));
        String url = a.get(0)+"/"+meetingName + "/" + a.get(3)+ "/" + a.get(2)+ "/" + a.get(5);
        return AppConstant.URL_TAP_RACE.replace(AppConstant.ID_PARAM, url);
    }
    public static String getURLRaceOfPointBet(String id,String meetingName,String countryCode, String racingTypeName){
        if(meetingName.contains(" ")){
            meetingName = meetingName.replace(" ","-");
        }
        String url = racingTypeName + "/" + countryCode + "/" + meetingName + "/race/" + id;
        return AppConstant.URL_POINT_BET_RACE.replace(AppConstant.ID_PARAM, url);
    }
    public static String getURLRaceOfSportBet(SportBetRacesData race,String raceType, String meetingName){
        String countryCode = convertCountryCode(race.getRegionGroup());
        int raceNumber = race.getRaceNumber();
        if(raceType.contains(" ")){
            raceType = raceType.replace(" ","-");
        }
        if(meetingName.contains(" ")){
            meetingName = meetingName.replace(" ","-");
        }
        String url = raceType.toLowerCase() + "/" + countryCode + "/" + meetingName.toLowerCase() + "/race-"+raceNumber+"-"+race.getId();
        return AppConstant.URL_SPORT_BET_RACE.replace(AppConstant.ID_PARAM, url);
    }
    public static String convertCountryCode(String countryCode) {
        switch (countryCode) {
            case "Aus/NZ":
                return "australia-nz";
            case "Hong Kong":
                return "hong-kong";
            default: return null;
        }
    }

    public static Optional<String> getLadbrokeRaceStatus(String ladbrokeStatusCode) {
        String status;
        switch (ladbrokeStatusCode) {
            case AppConstant.LADBROKE_STATUS_OPEN:
            case AppConstant.LADBROKE_STATUS_LIVE:
                status = AppConstant.STATUS_OPEN;
                break;
            case AppConstant.LADBROKE_STATUS_CLOSED:
                status = AppConstant.STATUS_CLOSED;
                break;
            case AppConstant.LADBROKE_STATUS_INTERIM:
                status = AppConstant.STATUS_INTERIM;
                break;
            case AppConstant.LADBROKE_STATUS_FINAL:
                status = AppConstant.STATUS_FINAL;
                break;
            case AppConstant.LADBROKE_STATUS_ABANDONED:
                status = AppConstant.STATUS_ABANDONED;
                break;
            default:
                status = null;
        }

        return Optional.ofNullable(status);
    }
    public static String getSideName(RaceBaseResponseDTO race){
        String sideName = AppConstant.SIDE_NAME_PREFIX + race.getNumber() + " " + race.getMeetingName();
        // Set side name for each race
        if (!race.getCountry().equals(AppConstant.AUS)) {
            sideName = sideName + " (" + race.getCountry() + ")";
        }
        return sideName;
    }

    public static String getLast6Race(String last20Starts) {
        int index = last20Starts.length();
        int last5Start = 5;
        if (index <= 6) {
            return last20Starts;
        }
        return last20Starts.substring(index - last5Start, index);
    }
    public static String convertRaceType(String raceType) {
        switch (raceType) {
            case AppConstant.GREYHOUND_RACING:
                return "GREYHOUND";
            case AppConstant.HORSE_RACING:
                return "HORSE";
            case AppConstant.HARNESS_RACING:
                return "HARNESS";
            default: return null;
        }
    }
}
