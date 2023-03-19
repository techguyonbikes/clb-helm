package com.tvf.clb.base.dto;

import com.google.gson.Gson;
import com.tvf.clb.base.entity.*;
import com.tvf.clb.base.model.EntrantRawData;
import com.tvf.clb.base.model.MeetingRawData;
import com.tvf.clb.base.model.RaceRawData;
import com.tvf.clb.base.utils.AppConstant;
import io.r2dbc.postgresql.codec.Json;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MeetingMapper {
    private static final Gson gson = new Gson();

    public static MeetingDto toMeetingDto(MeetingRawData meeting, List<RaceRawData> races) {
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
                .races(toRaceDtoList(races, meeting.getId()))
                .raceType(convertRaceType(meeting.getFeedId()))
                .build();
    }

    public static RaceDto toRaceDto(RaceRawData race, String meetingId) {
        return RaceDto.builder()
                .id(race.getId())
                .meetingUUID(meetingId)
                .name(race.getName())
                .number(race.getNumber())
                .advertisedStart(Instant.parse(race.getAdvertisedStart()))
                .actualStart(Instant.parse(race.getActualStart()))
                .marketIds(race.getMarketIds())
                .mainMarketStatusId(race.getMainMarketStatusId())
                .resultsDisplay(race.getResultsDisplay())
                .distance(race.getDistance())
                .build();
    }

    public static List<RaceDto> toRaceDtoList(List<RaceRawData> races, String meetingId) {
        List<RaceDto> raceDtoList = new ArrayList<>();
        races.forEach(r -> raceDtoList.add(toRaceDto(r, meetingId)));
        return raceDtoList;
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
            return null;
        }
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
                .resultsDisplay(raceDto.getResultsDisplay())
                .distance(raceDto.getDistance())
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
                .priceFluctuations(Json.of(gson.toJson(entrantRawData.getPriceFluctuations())))
                .isScratched(entrantRawData.getIsScratched() != null)
                .scratchedTime(entrantRawData.getScratchedTime())
                .position(entrantRawData.getPosition())
                .build();
    }



    public static EntrantSite toEntrantSite(Entrant entrant, Integer site, Long id) {
        return EntrantSite
                .builder()
                .siteId(site)
                .entrantSiteId(entrant.getEntrantId())
                .generalEntrantId(id)
                .priceFluctuations(entrant.getPriceFluctuations())
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
}
