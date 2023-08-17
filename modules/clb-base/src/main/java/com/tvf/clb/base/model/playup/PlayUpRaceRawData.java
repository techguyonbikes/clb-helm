package com.tvf.clb.base.model.playup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tvf.clb.base.utils.UpperCaseAndTrimStringDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayUpRaceRawData {
    @JsonProperty("race_number")
    private Integer raceNumber;
    @JsonProperty("name")
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String raceName;
    @JsonProperty("start_time")
    private String raceStartTime;
    @JsonProperty("status")
    private Status raceStatus;
    @JsonProperty("race_type")
    private RaceType raceType;
    @JsonProperty("distance")
    private Integer raceDistance;
    @JsonProperty("display_result")
    private String results;
    @JsonProperty("number_of_places")
    private Integer numberOfPlaces;
    @JsonProperty("number_of_runners")
    private Integer numberOfRunners;
    @JsonProperty("number_of_active_runners")
    private Integer numberOfActive;
    private Meeting meeting;
}
