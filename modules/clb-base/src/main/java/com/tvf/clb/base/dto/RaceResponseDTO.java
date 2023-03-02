package com.tvf.clb.base.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RaceResponseDTO {
    String raceId;
    String sideName;
    String type;
    String date;
}
