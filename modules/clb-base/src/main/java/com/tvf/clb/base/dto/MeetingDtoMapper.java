package com.tvf.clb.base.dto;

import com.tvf.clb.base.model.Meeting;
import com.tvf.clb.base.model.Race;
import com.tvf.clb.base.utils.AppConstant;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MeetingDtoMapper {

    public static MeetingDto toMeetingDto(Meeting meeting, List<Race> races) {
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
                .races(toRaceDtoList(races))
                .raceType(convertRaceType(meeting.getFeedId()))
                .build();
    }

    public static RaceDto toRaceDto(Race race) {
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

    public static List<RaceDto> toRaceDtoList(List<Race> races) {
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

}
