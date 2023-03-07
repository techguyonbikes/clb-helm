package com.tvf.clb.base.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class AdditionalInfoDto {
    private String raceId;
    private Integer distance;
    private String raceComment;
    private String  distanceType;
    private Integer generated;
    private String silkBaseUrl;
    private String trackCondition;
    private String weather;
}
