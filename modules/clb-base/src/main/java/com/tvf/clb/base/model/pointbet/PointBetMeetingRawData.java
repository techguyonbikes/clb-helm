package com.tvf.clb.base.model.pointbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tvf.clb.base.utils.UpperCaseAndTrimStringDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PointBetMeetingRawData {
    private String masterEventID;
    private String countryCode;
    private String countryName;
    private String firstRaceStartTimeUtc;
    private Integer racingType;
    private String racingTypeName;
    @JsonProperty("venue")
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String name;
    private Integer orderById;
    private Boolean hasRaceVision;
    private List<PointBetRacesRawData> races;
    private String version;
    private Boolean hasSameRaceMulti;
}
