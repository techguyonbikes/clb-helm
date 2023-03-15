package com.tvf.clb.base.dto;

import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import org.springframework.stereotype.Component;

@Component
public class RaceResponseMapper {

    private static final String SIDE_NAME_PREFIX = "R";

    public static RaceResponseDTO toRaceResponseDTO(Meeting meeting, Race race) {
        return RaceResponseDTO.builder()
                .raceId(race.getRaceId())
                .meetingId(meeting.getMeetingId())
                .sideName(SIDE_NAME_PREFIX + race.getNumber().toString() + " " + meeting.getName())
                .meetingName(meeting.getName())
                .number(race.getNumber())
                .type(meeting.getRaceType())
                .date(race.getActualStart())
                .raceName(race.getName())
                .distance(race.getDistance())
                .state(meeting.getState())
                .build();
    }
}
