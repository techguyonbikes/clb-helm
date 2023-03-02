package com.tvf.clb.base.dto;

import com.google.gson.Gson;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RaceResponseMapper {

    private static final Gson gson = new Gson();

    public RaceResponseDTO toRaceResponseDTO(Meeting meeting, Race race) {
        return RaceResponseDTO.builder()
                .raceId(race.getRaceId())
                .sideName(meeting.getRaceType().charAt(0) + race.getNumber() + meeting.getName())
                .type(meeting.getRaceType())
                .date(race.getActualStart())
                .build();
    }

}
