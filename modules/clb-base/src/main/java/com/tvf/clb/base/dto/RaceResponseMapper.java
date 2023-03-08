package com.tvf.clb.base.dto;

import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import org.springframework.stereotype.Component;

@Component
public class RaceResponseMapper {


    public static RaceResponseDTO toRaceResponseDTO(Meeting meeting, Race race) {
        return RaceResponseDTO.builder()
                .raceId(race.getRaceId())
                .sideName(meeting.getRaceType().charAt(0) + race.getNumber().toString() + " " + meeting.getName())
                .meetingName(meeting.getName())
                .number(race.getNumber())
                .type(meeting.getRaceType())
                .date(race.getActualStart())
                .raceName(race.getName())
                .distance(100)
                .state(meeting.getState())
                .build();
    }
}
