package com.tvf.clb.base.dto;

import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import org.springframework.stereotype.Component;

@Component
public class RaceResponseMapper {


    public static RaceResponseDTO toRaceResponseDTO(Meeting meeting, Race race) {
        return RaceResponseDTO.builder()
                .raceId(race.getRaceId())
                .sideName(meeting.getRaceType().charAt(meeting.getRaceType().indexOf(" ") + 1) + race.getNumber().toString() + " " + meeting.getName())
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
