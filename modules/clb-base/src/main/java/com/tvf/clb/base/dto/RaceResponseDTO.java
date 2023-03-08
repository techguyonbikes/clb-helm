package com.tvf.clb.base.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class RaceResponseDTO {
    String raceId;
    String sideName;
    String meetingName;
    Integer number;
    String type;
    Instant date;
    String state;
    String stateName;
    String raceName;
    Integer distance;
}
