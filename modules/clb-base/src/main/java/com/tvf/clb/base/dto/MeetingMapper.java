package com.tvf.clb.base.dto;

import com.google.gson.Gson;
import com.tvf.clb.base.dto.sportbet.SportBetMeetingDto;
import com.tvf.clb.base.dto.topsport.TopSportMeetingDto;
import com.tvf.clb.base.dto.topsport.TopSportRaceDto;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.MeetingSite;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.model.EntrantRawData;
import com.tvf.clb.base.model.MeetingRawData;
import com.tvf.clb.base.model.RaceRawData;
import com.tvf.clb.base.model.pointbet.PointBetMeetingRawData;
import com.tvf.clb.base.model.pointbet.PointBetRacesRawData;
import com.tvf.clb.base.model.sportbet.SportBetEntrantRawData;
import com.tvf.clb.base.model.sportbet.SportBetMeetingRawData;
import com.tvf.clb.base.model.sportbet.SportBetRacesData;
import com.tvf.clb.base.model.tab.TabMeetingRawData;
import com.tvf.clb.base.model.tab.TabRacesData;
import com.tvf.clb.base.model.zbet.ZBetEntrantData;
import com.tvf.clb.base.model.zbet.ZBetMeetingRawData;
import com.tvf.clb.base.model.zbet.ZBetRacesData;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.CommonUtils;
import com.tvf.clb.base.utils.ConvertBase;
import io.r2dbc.postgresql.codec.Json;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor(access= AccessLevel.PRIVATE)
public class MeetingMapper {
    private static final Gson gson = new Gson();

    public static MeetingDto toMeetingDto(MeetingRawData meeting, List<RaceRawData> races) {
        String raceType = convertRaceType(meeting.getFeedId());
        return MeetingDto.builder()
                .id(meeting.getId())
                .name(meeting.getName())
                .advertisedDate(Instant.parse(meeting.getAdvertisedDate()))
                .categoryId(meeting.getCategoryId())
                .venueId(meeting.getVenueId())
                .trackCondition(meeting.getTrackCondition())
                .country(meeting.getCountry())
                .state(meeting.getState())
                .hasFixed(meeting.isHasFixed())
                .regionId(meeting.getRegionId())
                .feedId(meeting.getFeedId())
                .compoundIds(meeting.getCompoundIds())
                .races(toRaceDtoList(races, meeting.getId(), meeting.getName(), raceType))
                .raceType(raceType)
                .build();
    }

    public static MeetingDto toMeetingDto(PointBetMeetingRawData meeting) {
        String raceType = convertRaceTypePointBet(meeting.getRacingType());
        return MeetingDto.builder()
                .id(meeting.getMasterEventID())
                .name(meeting.getName())
                .country(meeting.getCountryCode())
                .advertisedDate(Instant.parse(meeting.getFirstRaceStartTimeUtc()))
                .races(toRaceDtoListPointBet(meeting.getRaces(), meeting.getMasterEventID(), meeting.getName(), raceType))
                .raceType(raceType)
                .build();
    }

    public static RaceDto toRaceDto(RaceRawData race, String meetingUUID, String meetingName, String raceType) {
        return RaceDto.builder()
                .id(race.getId())
                .meetingUUID(meetingUUID)
                .meetingName(meetingName)
                .name(race.getName())
                .number(race.getNumber())
                .raceType(raceType)
                .advertisedStart(Instant.parse(race.getAdvertisedStart()))
                .actualStart(Instant.parse(race.getActualStart()))
                .marketIds(race.getMarketIds())
                .mainMarketStatusId(race.getMainMarketStatusId())
                .resultsDisplay(race.getResultsDisplay())
                .distance(race.getDistance())
                .build();
    }

    public static List<RaceDto> toRaceDtoList(List<RaceRawData> races, String meetingUUID, String meetingName, String raceType) {
        List<RaceDto> raceDtoList = new ArrayList<>();
        races.forEach(r -> raceDtoList.add(toRaceDto(r, meetingUUID, meetingName, raceType)));
        return raceDtoList;
    }

    public static List<RaceDto> toRaceDtoListPointBet(List<PointBetRacesRawData> races, String meetingUUID, String meetingName, String raceType) {
        return races.stream().map(race -> toRaceDto(race, meetingUUID, meetingName, raceType)).collect(Collectors.toList());
    }

    public static RaceDto toRaceDto(PointBetRacesRawData race, String meetingUUID, String meetingName, String raceType) {
        return RaceDto.builder()
                .id(race.getEventId())
                .meetingUUID(meetingUUID)
                .meetingName(meetingName)
                .name(race.getName())
                .raceType(raceType)
                .number(race.getRaceNumber())
                .advertisedStart(Instant.parse(race.getAdvertisedStartDateTime()))
                .actualStart(Instant.parse(race.getAdvertisedStartDateTime()))
                .status(ConvertBase.getRaceStatusById(race.getTradingStatus(),race.getResultStatus()))
                .build();
    }

    public static RaceDto toRaceDto(ZBetRacesData race) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(AppConstant.DATE_TIME_PATTERN);
        return RaceDto.builder()
                .id(race.getId().toString())
                .meetingName(race.getMeetingName())
                .name(race.getName())
                .raceType(race.getType())
                .number(race.getNumber())
                .status(race.getStatus())
                .advertisedStart(LocalDateTime.parse(race.getStartDate(), dtf).atZone(AppConstant.AU_ZONE_ID).toInstant())
                .build();
    }

    public static RaceDto toRaceDto(ZBetRacesData race, Integer distance) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(AppConstant.DATE_TIME_PATTERN);
        return RaceDto.builder()
                .id(race.getId().toString())
                .meetingName(race.getMeetingName())
                .name(race.getName())
                .raceType(race.getType())
                .number(race.getNumber())
                .status(race.getStatus())
                .distance(distance)
                .advertisedStart(LocalDateTime.parse(race.getStartDate(), dtf).atZone(AppConstant.AU_ZONE_ID).toInstant())
                .build();
    }

    public static String convertRaceType(String feedId) {
        if (feedId.contains(AppConstant.GREYHOUND_FEED_TYPE)) {
            return AppConstant.GREYHOUND_RACING;
        } else if (feedId.contains(AppConstant.HORSE_FEED_TYPE)) {
            return AppConstant.HORSE_RACING;
        }else if (feedId.contains(AppConstant.HARNESS_FEED_TYPE)) {
            return AppConstant.HARNESS_RACING;
        }
        else {
            return AppConstant.HORSE_RACING; // racetype is null in labroker  = horse racing in other site
        }
    }

    public static String convertRaceTypePointBet(Integer raceTypeId) {
        String raceTypeName = null;
        switch (raceTypeId) {
            case 1:
                raceTypeName = AppConstant.HORSE_RACING;
                break;
            case 2:
                raceTypeName = AppConstant.HARNESS_RACING;
                break;
            case 4:
                raceTypeName = AppConstant.GREYHOUND_RACING;
                break;
        }
        return raceTypeName;
    }

    public static Meeting toMeetingEntity(MeetingRawData meeting) {
        return Meeting.builder()
                .meetingId(meeting.getId())
                .name(meeting.getName())
                .advertisedDate(Instant.parse(meeting.getAdvertisedDate()))
                .categoryId(meeting.getCategoryId())
                .venueId(meeting.getVenueId())
                .trackCondition(meeting.getTrackCondition())
                .country(meeting.getCountry())
                .state(meeting.getState())
                .hasFixed(meeting.isHasFixed())
                .regionId(meeting.getRegionId())
                .feedId(meeting.getFeedId())
                .raceType(convertRaceType(meeting.getFeedId()))
                .build();
    }

    public static Meeting toMeetingEntity(MeetingDto meeting) {
        return Meeting.builder()
                .meetingId(meeting.getId())
                .name(meeting.getName())
                .advertisedDate(meeting.getAdvertisedDate())
                .categoryId(meeting.getCategoryId())
                .venueId(meeting.getVenueId())
                .trackCondition(meeting.getTrackCondition())
                .country(meeting.getCountry())
                .state(meeting.getState())
                .hasFixed(meeting.getHasFixed())
                .regionId(meeting.getRegionId())
                .feedId(meeting.getFeedId())
                .raceType(meeting.getRaceType())
                .build();
    }

    public static Race toRaceEntity(RaceDto raceDto) {
        return Race.builder()
                .raceId(raceDto.getId())
                .meetingId(raceDto.getMeetingId())
                .meetingUUID(raceDto.getMeetingUUID())
                .name(raceDto.getName())
                .number(raceDto.getNumber())
                .advertisedStart(raceDto.getAdvertisedStart())
                .actualStart(raceDto.getActualStart())
                .marketIds(Json.of(gson.toJson(raceDto.getMarketIds())))
                .mainMarketStatusId(raceDto.getMainMarketStatusId())
                .distance(raceDto.getDistance())
                .raceType(raceDto.getRaceType())
                .build();
    }
    public static Entrant toEntrantEntity(EntrantRawData entrantRawData) {
        return Entrant.builder()
                .entrantId(entrantRawData.getId())
                .raceUUID(entrantRawData.getRaceId())
                .name(entrantRawData.getName())
                .number(entrantRawData.getNumber())
                .barrier(entrantRawData.getBarrier())
                .visible(entrantRawData.isVisible())
                .marketId(entrantRawData.getMarketId())
                .currentSitePrice(entrantRawData.getPriceFluctuations())
                .isScratched(entrantRawData.getIsScratched() != null)
                .scratchedTime(entrantRawData.getScratchedTime())
                .position(entrantRawData.getPosition())
                .build();
    }
    public static MeetingSite toMetingSite(Meeting meeting, Integer siteId,Long generalId) {
        return MeetingSite.builder()
                .meetingSiteId(meeting.getMeetingId())
                .generalMeetingId(generalId)
                .siteId(siteId)
                .startDate(meeting.getAdvertisedDate())
                .build();
    }
    public static MeetingDto toMeetingTABDto(TabMeetingRawData meeting, List<TabRacesData> races ) {
        String raceType = ConvertBase.convertRaceTypeOfTab(meeting.getRaceType());
        return MeetingDto.builder()
                .id(getMeetingId(meeting))
                .name(meeting.getMeetingName())
                .advertisedDate(ConvertBase.dateFormat(meeting.getMeetingDate()))
                .state(meeting.getLocation())
                .raceType(raceType)
                .races(toRaceDtoListFromTab(races,getMeetingId(meeting), meeting.getMeetingName(), raceType, meeting.getLocation()))
                .build();
    }

    public static Meeting toMeetingEntityFromTab(TabMeetingRawData meeting) {
        return Meeting.builder()
                .meetingId(getMeetingId(meeting))
                .name(meeting.getMeetingName())
                .state(meeting.getLocation())
                .advertisedDate(ConvertBase.dateFormat(meeting.getMeetingDate()))
                .raceType(ConvertBase.convertRaceTypeOfTab(meeting.getRaceType()))
                .build();
    }

    public static List<RaceDto> toRaceDtoListFromTab(List<TabRacesData> races, String meetingId, String meetingName, String raceType, String location) {
        List<RaceDto> raceDtoList = new ArrayList<>();
        races.forEach(r -> raceDtoList.add(toRaceDtoByTab(r, meetingId, meetingName, raceType, location)));
        return raceDtoList;
    }

    public static RaceDto toRaceDtoByTab(TabRacesData race, String meetingId, String meetingName, String raceType, String location) {
        return RaceDto.builder()
                .id(getRaceId(meetingId, race))
                .meetingUUID(meetingId)
                .meetingName(meetingName)
                .name(race.getRaceName())
                .raceType(raceType)
                .number(race.getRaceNumber())
                .advertisedStart(Instant.parse(race.getRaceStartTime()))
                .distance(race.getRaceDistance())
                .build();
    }

    public static Entrant toEntrantEntity(EntrantRawData entrantRawData, Integer site) {
        return Entrant.builder()
                .entrantId(entrantRawData.getId())
                .raceUUID(entrantRawData.getRaceId())
                .name(entrantRawData.getName())
                .number(entrantRawData.getNumber())
                .barrier(entrantRawData.getBarrier())
                .visible(entrantRawData.isVisible())
                .marketId(entrantRawData.getMarketId())
                .priceFluctuations(Json.of(gson.toJson(Collections.singletonMap(site, CommonUtils.convertToPriceHistoryData(entrantRawData.getPriceFluctuations())))))
                .isScratched(entrantRawData.getIsScratched() != null)
                .scratchedTime(entrantRawData.getScratchedTime())
                .position(entrantRawData.getPosition())
                .riderOrDriver(entrantRawData.getFormSummary().getRiderOrDriver())
                .trainerName(entrantRawData.getFormSummary().getTrainerName())
                .build();
    }

    public static Meeting toMeetingEntity(ZBetMeetingRawData meeting) {
        //only get date of startDate
        DateTimeFormatter sdf = DateTimeFormatter.ofPattern(AppConstant.DATE_PATTERN);
        String startDateString = meeting.getStartDate().substring(0, 10);

        return Meeting.builder()
                .meetingId(meeting.getMeetingId())
                .name(meeting.getName())
                .state(meeting.getState())
                .advertisedDate(LocalDate.parse(startDateString, sdf).atStartOfDay(AppConstant.UTC_ZONE_ID).toInstant())
                .raceType(convertRacesType(meeting.getType()))
                .build();
    }

    public static Race toRaceEntity(ZBetRacesData race) {
        //only get date of startDate
        DateTimeFormatter sdf = DateTimeFormatter.ofPattern(AppConstant.DATE_TIME_PATTERN);

        return Race.builder()
                .raceId(race.getId().toString())
                .name(race.getName())
                .number(race.getNumber())
                .raceType(race.getType())
                .advertisedStart(LocalDateTime.parse(race.getStartDate(), sdf).atZone(AppConstant.AU_ZONE_ID).toInstant())
                .build();
    }

    public static Entrant toEntrantEntity(ZBetEntrantData entrant, List<Float> prices) {
        return Entrant.builder()
                .entrantId(entrant.getId().toString())
                .name(entrant.getName())
                .number(entrant.getNumber())
                .barrier(entrant.getBarrier())
                .currentSitePrice(prices)
                .build();
    }

    public static String convertRacesType(String feedId) {
        if (AppConstant.GREYHOUND_TYPE_RACE.contains(feedId)) {
            return AppConstant.GREYHOUND_RACING;
        } else if (AppConstant.HORSE_TYPE_RACE.contains(feedId)) {
            return AppConstant.HORSE_RACING;
        } else if (AppConstant.HARNESS_TYPE_RACE.contains(feedId)) {
            return AppConstant.HARNESS_RACING;
        } else {
            return null;
        }
    }
    public static String getMeetingId(TabMeetingRawData meeting){
        return meeting.getMeetingDate()+"/meetings/"+meeting.getRaceType()+"/"+meeting.getVenueMnemonic();
    }
    public static String getRaceId(String meetingId, TabRacesData race){
        return meetingId +"/races/"+ race.getRaceNumber();
    }

    public static List<MeetingDto> toMeetingSportDtoList(SportBetMeetingDto sportBetMeetingDto, List<SportBetMeetingRawData> meetingRawData, LocalDate date) {
        List<MeetingDto> meetingDtoList = new ArrayList<>();
        String raceType = ConvertBase.convertRaceTypeOfSportBet(sportBetMeetingDto.getRaceType());
        meetingRawData.forEach(r -> meetingDtoList.add(toMeetingdToBySport(r, raceType, date)));
        return meetingDtoList;
    }

    public static MeetingDto toMeetingdToBySport(SportBetMeetingRawData meeting, String raceType,LocalDate date) {
        return MeetingDto.builder()
                .id(meeting.getId().toString())
                .name(meeting.getName())
                .advertisedDate(ConvertBase.dateFormat(date))
                //.state(meeting.getLocation())
                .raceType(raceType)
                .races(toRaceDtoListFromSport(meeting, raceType))
                .build();
    }
    public static List<RaceDto> toRaceDtoListFromSport(SportBetMeetingRawData meeting, String raceType) {
        List<RaceDto> raceDtoList = new ArrayList<>();
        List<SportBetRacesData> races = meeting.getEvents();
        races.forEach(r -> raceDtoList.add(toRaceDtoByTab(r,meeting, raceType)));
        return raceDtoList;
    }

    public static RaceDto toRaceDtoByTab(SportBetRacesData race,SportBetMeetingRawData meeting, String raceType) {
        return RaceDto.builder()
                .id(race.getId().toString())
                .meetingUUID(meeting.getId().toString())
                .meetingName(meeting.getName())
                .name(race.getName())
                .raceType(raceType)
                .number(race.getRaceNumber())
                .advertisedStart(Instant.ofEpochMilli(race.getStartTime()*1000))
                .distance(race.getDistance())
                .build();
    }
    public static Entrant toEntrantEntity(SportBetEntrantRawData entrant, List<Float> prices) {
        return Entrant.builder()
                .entrantId(entrant.getId().toString())
                .name(entrant.getName())
                .number(entrant.getRunnerNumber())
                .barrier(entrant.getDrawNumber())
                .currentSitePrice(prices)
                .build();
    }
    public static Meeting toMeetingEntityFromTOP(TopSportMeetingDto meeting) {
        return Meeting.builder()
                .meetingId(meeting.getId())
                .name(meeting.getName().toUpperCase().trim())
                .state(meeting.getState())
                .advertisedDate(meeting.getAdvertisedDate())
                .raceType(ConvertBase.convertRaceTypeOfTOP(meeting.getRaceType()))
                .build();
    }
    public static Race toRaceEntityFromTOP(TopSportRaceDto race) {
        return Race.builder()
                .raceId(race.getId())
                .name(race.getRaceName())
                .number(race.getRaceNumber())
                .raceType(race.getRaceType())
                .advertisedStart(race.getStartTime())
                .distance(race.getDistance())
                .build();
    }
    public static MeetingDto toMeetingDtoFromTOP(TopSportMeetingDto meeting){
        return MeetingDto.builder()
                .id(meeting.getId())
                .name(meeting.getName())
                .country(meeting.getCountry())
                .advertisedDate(meeting.getAdvertisedDate())
                .raceType(meeting.getRaceType())
                .build();
    }
}
