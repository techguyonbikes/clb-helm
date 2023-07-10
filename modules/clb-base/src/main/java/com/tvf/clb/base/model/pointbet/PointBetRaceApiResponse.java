package com.tvf.clb.base.model.pointbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tvf.clb.base.utils.UpperCaseAndTrimStringDeserializer;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PointBetRaceApiResponse {

    @JsonProperty("outcomes")
    List<PointBetEntrantRawData> entrants;
    private String trackCondition;
    private Integer raceDistance;
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String name;
    private String countryCode;
    private Integer racingType;
    private String advertisedStartDateTime;
    private String placing;
    private Integer tradingStatus;
    private Integer resultStatus;
    private PointBetResultRawData results;
}
