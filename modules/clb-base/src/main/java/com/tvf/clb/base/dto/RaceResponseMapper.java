package com.tvf.clb.base.dto;

import com.google.gson.Gson;
import com.tvf.clb.base.entity.AdditionalInfo;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.model.RaceRawData;
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
                .distance("500m")
                .state(meeting.getState())
                .build();
    }
    public static AdditionalInfo toAdditionalInfo(RaceRawData raceRawData , String id) {
        return AdditionalInfo.builder()
                .distance(raceRawData.getAdditionalInfo().getDistance())
                .raceId(id)
                .generated(raceRawData.getAdditionalInfo().getGenerated())
                .trackCondition(raceRawData.getAdditionalInfo().getTrackCondition().getName())
                .weather(raceRawData.getAdditionalInfo().getWeather().getName())
                .raceComment(raceRawData.getAdditionalInfo().getRaceComment())
                .silkBaseUrl(raceRawData.getAdditionalInfo().getSilkBaseUrl())
                .build();
    }

}
