package com.tvf.clb.base.dto;

import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.model.MeetingRawData;
import com.tvf.clb.base.model.RaceRawData;
import com.tvf.clb.base.utils.AppConstant;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MeetingMapper {

    public static MeetingDto toMeetingDto(MeetingRawData meeting, List<RaceRawData> races) {
        return MeetingDto.builder()
                .id(meeting.getId().toString())
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
                .races(toRaceDtoList(races))
                .raceType(convertRaceType(meeting.getFeedId()))
                .build();
    }

    public static RaceDto toRaceDto(RaceRawData race) {
        return RaceDto.builder()
                .id(race.getId())
                .meetingId(race.getMeetingId())
                .name(race.getName())
                .number(race.getNumber())
                .advertisedStart(race.getAdvertisedStart())
                .actualStart(race.getActualStart())
                .marketIds(race.getMarketIds())
                .mainMarketStatusId(race.getMainMarketStatusId())
                .resultsDisplay(race.getResultsDisplay())
                .build();
    }

    public static List<RaceDto> toRaceDtoList(List<RaceRawData> races) {
        List<RaceDto> raceDtoList = new ArrayList<>();
        races.stream().forEach((role) -> {
            raceDtoList.add(toRaceDto(role));
        });
        return raceDtoList;
    }

    private static String convertRaceType(String feedId) {
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

}
