package com.tvf.clb.base.model.colossalbet;

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
public class ColBetMeetingRawData {
    @JsonProperty("meetingId")
    private Long id;
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    @JsonProperty("meetingName")
    private String name;

    @JsonProperty("race_state")
    private String raceState;

    @JsonProperty("sportCode")
    private String raceType;

    private String country;
    private String raceDay;

    private List<ColBetRaceRawData> races;
}
