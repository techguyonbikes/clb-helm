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
import com.tvf.clb.base.model.betm.BetMMeetingRawData;
import com.tvf.clb.base.model.betm.BetMRaceRawData;
import com.tvf.clb.base.model.betm.BetMRaceStatusEnum;
import com.tvf.clb.base.model.betm.BetMRaceTypeEnum;
import com.tvf.clb.base.model.colossalbet.ColBetMeetingRawData;
import com.tvf.clb.base.model.colossalbet.ColBetRaceRawData;
import com.tvf.clb.base.model.colossalbet.ColBetRaceTypeEnum;
import com.tvf.clb.base.model.playup.*;
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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@NoArgsConstructor(access= AccessLevel.PRIVATE)
public class MeetingMapper {
    private static final Gson gson = new Gson();

    public static MeetingDto toMeetingDto(MeetingRawData meeting, List<RaceRawData> races) {
        String raceType = ConvertBase.convertRaceTypeByFeedId(meeting.getFeedId());
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
                .races(toRaceDtoList(races, meeting, raceType))
                .raceType(raceType)
                .build();
    }

    public static MeetingDto toMeetingDto(PointBetMeetingRawData meeting) {
        String raceType = ConvertBase.convertRaceTypePointBet(meeting.getRacingType());
        return MeetingDto.builder()
                .id(meeting.getMasterEventID())
                .name(meeting.getName())
                .country(meeting.getCountryCode())
                .advertisedDate(Instant.parse(meeting.getFirstRaceStartTimeUtc()))
                .races(toRaceDtoListPointBet(meeting.getRaces(), meeting.getMasterEventID(), meeting.getName(), raceType, meeting.getCountryCode(), meeting.getRacingTypeName()))
                .raceType(raceType)
                .build();
    }

    public static RaceDto toRaceDto(RaceRawData race, MeetingRawData meeting, String raceType) {
        return RaceDto.builder()
                .id(race.getId())
                .meetingUUID(meeting.getId())
                .meetingName(meeting.getName())
                .status(ConvertBase.getLadbrokeRaceStatus(race.getMainMarketStatusId()).orElse(null))
                .name(race.getName())
                .number(race.getNumber())
                .raceType(raceType)
                .advertisedStart(Instant.parse(race.getAdvertisedStart()))
                .actualStart(Instant.parse(race.getActualStart()))
                .marketIds(race.getMarketIds())
                .mainMarketStatusId(race.getMainMarketStatusId())
                .resultsDisplay(race.getResultsDisplay())
                .distance(race.getDistance())
                .raceSiteUrl(ConvertBase.getURLRaceOfLadbrokes(meeting.getName(),race.getId()))
                .countryCode(meeting.getCountry())
                .state(meeting.getState())
                .build();
    }

    public static List<RaceDto> toRaceDtoList(List<RaceRawData> races, MeetingRawData meeting, String raceType) {
        List<RaceDto> raceDtoList = new ArrayList<>();
        races.forEach(r -> raceDtoList.add(toRaceDto(r, meeting, raceType)));
        return raceDtoList;
    }

    public static List<RaceDto> toRaceDtoListPointBet(List<PointBetRacesRawData> races, String meetingUUID, String meetingName, String raceType, String countryCode, String racingTypeName) {
        return races.stream().map(race -> toRaceDto(race, meetingUUID, meetingName, raceType, countryCode, racingTypeName)).collect(Collectors.toList());
    }

    public static RaceDto toRaceDto(PointBetRacesRawData race, String meetingUUID, String meetingName, String raceType,String countryCode, String racingTypeName) {
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
                .raceSiteUrl(ConvertBase.getURLRaceOfPointBet(race.getEventId(),meetingName, countryCode,racingTypeName))
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
                .raceSiteUrl(race.getRaceSiteLink())
                .build();
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
                .raceType(ConvertBase.convertRaceTypeByFeedId(meeting.getFeedId()))
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
                .status(raceDto.getStatus())
                .actualStart(raceDto.getActualStart())
                .marketIds(Json.of(gson.toJson(raceDto.getMarketIds())))
                .mainMarketStatusId(raceDto.getMainMarketStatusId())
                .distance(raceDto.getDistance())
                .raceType(raceDto.getRaceType())
                .raceSiteUrl(raceDto.getRaceSiteUrl())
                .meetingName(raceDto.getMeetingName())
                .venueId(raceDto.getVenueId())
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
                .currentSitePricePlaces(entrantRawData.getPricePlaces())
                .isScratched(entrantRawData.getIsScratched() != null)
                .scratchedTime(entrantRawData.getScratchedTime())
                .position(entrantRawData.getPosition())
                .currentWinDeductions(entrantRawData.getWinDeduction() == null ? null : entrantRawData.getWinDeduction())
                .currentPlaceDeductions(entrantRawData.getPlaceDeduction() == null ? null : entrantRawData.getPlaceDeduction())
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
                .races(toRaceDtoListFromTab(races,getMeetingId(meeting), meeting.getMeetingName(), raceType))
                .build();
    }

    public static List<RaceDto> toRaceDtoListFromTab(List<TabRacesData> races, String meetingId, String meetingName, String raceType) {
        List<RaceDto> raceDtoList = new ArrayList<>();
        races.forEach(r -> raceDtoList.add(toRaceDtoByTab(r, meetingId, meetingName, raceType)));
        return raceDtoList;
    }

    public static RaceDto toRaceDtoByTab(TabRacesData race, String meetingId, String meetingName, String raceType) {
        return RaceDto.builder()
                .id(getRaceId(meetingId, race))
                .meetingUUID(meetingId)
                .meetingName(meetingName)
                .name(race.getRaceName())
                .raceType(raceType)
                .number(race.getRaceNumber())
                .advertisedStart(Instant.parse(race.getRaceStartTime()))
                .distance(race.getRaceDistance())
                .raceSiteUrl(ConvertBase.getURLRaceOfTAP(getRaceId(meetingId, race), meetingName))
                .build();
    }

    public static Entrant toEntrantEntity(EntrantRawData entrantRawData, Integer site) {
        Map<Integer, Float> winDeductions = entrantRawData.getWinDeduction() == null ? Collections.emptyMap() : Collections.singletonMap(site, entrantRawData.getWinDeduction());
        Map<Integer, Float> placeDeductions = entrantRawData.getPlaceDeduction() == null ? Collections.emptyMap() : Collections.singletonMap(site, entrantRawData.getPlaceDeduction());

        return Entrant.builder()
                .entrantId(entrantRawData.getId())
                .raceUUID(entrantRawData.getRaceId())
                .name(entrantRawData.getName())
                .number(entrantRawData.getNumber())
                .barrier(entrantRawData.getBarrier())
                .visible(entrantRawData.isVisible())
                .marketId(entrantRawData.getMarketId())
                .priceFluctuations(Json.of(gson.toJson(Collections.singletonMap(site, CommonUtils.convertToPriceHistoryData(entrantRawData.getPriceFluctuations())))))
                .pricePlaces(Json.of(gson.toJson(Collections.singletonMap(site, CommonUtils.convertToPriceHistoryData(entrantRawData.getPricePlaces())))))
                .isScratched(entrantRawData.getIsScratched() != null)
                .scratchedTime(entrantRawData.getScratchedTime())
                .position(entrantRawData.getPosition())
                .riderOrDriver(entrantRawData.getFormSummary().getRiderOrDriver())
                .trainerName(entrantRawData.getFormSummary().getTrainerName())
                .last6Starts(ConvertBase.getLast6Race(entrantRawData.getFormSummary().getLast20Starts()))
                .handicapWeight(entrantRawData.getFormSummary().getHandicapWeight())
                .bestTime(entrantRawData.getFormSummary().getBestTime())
                .entrantComment(entrantRawData.getFormSummary().getEntrantComment())
                .bestMileRate(entrantRawData.getFormSummary().getBestMileRate())
                .barrierPosition(entrantRawData.getBarrierPosition())
                .priceWinDeductions(CommonUtils.toJsonb(winDeductions))
                .pricePlaceDeductions(CommonUtils.toJsonb(placeDeductions))
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
                .raceType(ConvertBase.convertRacesTypeZbet(meeting.getType()))
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
                .status(race.getStatus())
                .raceSiteUrl(race.getRaceSiteLink())
                .advertisedStart(LocalDateTime.parse(race.getStartDate(), sdf).atZone(AppConstant.AU_ZONE_ID).toInstant())
                .build();
    }

    public static Entrant toEntrantEntity(ZBetEntrantData entrant, List<Float> pricesFixed, List<Float> pricePlaces) {
        return Entrant.builder()
                .entrantId(entrant.getId().toString())
                .name(entrant.getName())
                .number(entrant.getNumber())
                .barrier(entrant.getBarrier())
                .currentSitePrice(pricesFixed)
                .currentSitePricePlaces(pricePlaces)
                .currentPlaceDeductions(entrant.getPlaceDeductions() / 100)
                .currentWinDeductions(entrant.getWinDeductions() / 100)
                .build();
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
        meetingRawData.forEach(r -> meetingDtoList.add(toMeetingDtoBySport(r, raceType, date)));
        return meetingDtoList;
    }

    public static MeetingDto toMeetingDtoBySport(SportBetMeetingRawData meeting, String raceType,LocalDate date) {
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
                .raceSiteUrl(ConvertBase.getURLRaceOfSportBet(race, raceType, meeting.getName()))
                .build();
    }
    public static Entrant toEntrantEntity(SportBetEntrantRawData entrant, List<Float> winPrices, List<Float> placePrices) {
        return Entrant.builder()
                .entrantId(entrant.getId().toString())
                .name(entrant.getName())
                .number(entrant.getRunnerNumber())
                .barrier(entrant.getDrawNumber())
                .currentSitePrice(winPrices)
                .currentSitePricePlaces(placePrices)
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
                .raceSiteUrl(AppConstant.URL_TOPSPORT_RACE.replace(AppConstant.ID_PARAM, race.getId().replace("_","-")))
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
    public static Race toRaceEntityFromNED(RaceDto raceDto) {
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
                .raceSiteUrl(ConvertBase.getURLRaceOfNEDS(raceDto))
                .meetingName(raceDto.getMeetingName())
                .build();
    }
    public static MeetingDto toMeetingPlayUpDto(PlayUpMeetingDtoRawData playUpMeetingDtoRawData) {
        PlayUpMeetingRawData playUpMeetingRawData = new PlayUpMeetingRawData();
        String raceId = null;
        if (playUpMeetingDtoRawData != null){
            playUpMeetingRawData = playUpMeetingDtoRawData.getAttributes();
            raceId = playUpMeetingDtoRawData.getId().toString();
        }
        String raceType = ConvertBase.convertRaceTypeOfPlayUp(playUpMeetingRawData.getRaceType().getName());
        Country country = playUpMeetingRawData.getCountry();
        return MeetingDto.builder()
                .id(raceId)
                .name(playUpMeetingRawData.getName())
                .advertisedDate(ConvertBase.dateFormat(playUpMeetingRawData.getMeetingDate()))
                .state(playUpMeetingRawData.getState())
                .raceType(raceType)
                .country(country == null ? null :country.getCode())
                .build();
    }
    public static RaceDto toRacePlayUpDto(PlayUpRaceDtoRawData playUpRaceDtoRawData) {
        PlayUpRaceRawData raceRawData = new PlayUpRaceRawData();
        String meetingId = null;
        if (playUpRaceDtoRawData != null){
            raceRawData = playUpRaceDtoRawData.getAttributes();
            meetingId = playUpRaceDtoRawData.getId().toString();
        }
        return RaceDto.builder()
                .id(meetingId)
                .meetingUUID(raceRawData.getMeeting().getId().toString())
                .meetingName(raceRawData.getMeeting().getName())
                .name(raceRawData.getRaceName())
                .raceType(ConvertBase.convertRaceTypeOfPlayUp(raceRawData.getRaceType().getName()))
                .number(raceRawData.getRaceNumber())
                .advertisedStart(Instant.parse(ZonedDateTime.parse(raceRawData.getRaceStartTime()).toInstant().toString()))
                .distance(raceRawData.getRaceDistance())
                .raceSiteUrl(ConvertBase.getURLRaceOfPlayUp(meetingId,raceRawData.getMeeting().getName(), raceRawData.getRaceType().getName(), raceRawData.getRaceNumber()))
                .build();
    }

    public static MeetingDto toMeetingDto(BetMMeetingRawData betMMeetingRawData) {
        String raceType = BetMRaceTypeEnum.getValueFromRawData(betMMeetingRawData.getRaceType());
        return MeetingDto.builder()
                .id(UUID.randomUUID().toString())
                .name(betMMeetingRawData.getName())
                .state(betMMeetingRawData.getRaceState())
                .raceType(raceType)
                .races(betMMeetingRawData.getRaces().stream().map(race -> MeetingMapper.toRaceDto(race, betMMeetingRawData.getName(), raceType)).collect(Collectors.toList()))
                .build();
    }

    public static RaceDto toRaceDto(BetMRaceRawData betMRaceRawData, String meetingName, String raceType) {
        return RaceDto.builder()
                .id(betMRaceRawData.getId().toString())
                .name(betMRaceRawData.getName())
                .number(betMRaceRawData.getNumber())
                .advertisedStart(betMRaceRawData.getStartTime())
                .status(BetMRaceStatusEnum.getValueFromRawData(betMRaceRawData.getStatus()))
                .finalResult(betMRaceRawData.getResult())
                .raceType(raceType)
                .raceSiteUrl(AppConstant.URL_BET_M_RACE.replace(AppConstant.ID_PARAM, String.format("%s/%s", meetingName, betMRaceRawData.getId())))
                .build();
    }
    public static MeetingDto toMeetingDto(ColBetMeetingRawData meetingRawData) {
        String raceType = ColBetRaceTypeEnum.getValueFromRawData(meetingRawData.getRaceType());
        return MeetingDto.builder()
                .id(meetingRawData.getId().toString())
                .name(meetingRawData.getName())
                .advertisedDate((ConvertBase.getStringInstantDate(meetingRawData.getRaceDay())))
                .raceType(raceType)
                .races(meetingRawData.getRaces().stream().map(race -> MeetingMapper.toRaceDto(race, raceType)).collect(Collectors.toList()))
                .build();
    }
    public static RaceDto toRaceDto(ColBetRaceRawData raceRawData, String raceType) {
        return RaceDto.builder()
                .id(raceRawData.getId().toString())
                .name("Race"+raceRawData.getNumber())
                .number(raceRawData.getNumber())
                .advertisedStart(ConvertBase.getStringInstantRaceDate(raceRawData.getStartTime()))
                .status(raceRawData.getStatus())
                .finalResult(raceRawData.getResult())
                .raceType(raceType)
                .raceSiteUrl(AppConstant.URL_COLOSSAL_BET_RACE.replace(AppConstant.ID_PARAM, raceRawData.getId().toString()))
                .build();
    }
}
