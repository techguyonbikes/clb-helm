package com.tvf.clb.base.dto;

import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;
import java.time.Instant;

@Data
@Builder
public class RaceResponseDTO {
    String raceId;
    String sideName;
    String type;
    Instant date;
}
