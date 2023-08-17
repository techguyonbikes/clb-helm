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
public class PlayUpMeetingRawData {
    @JsonProperty("name")
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String name;
    private String location;
    @JsonProperty("race_type")
    private RaceType raceType;
    @JsonProperty("start_date")
    private String meetingDate;
    private String state;
    private String weather;
    private TrackCondition track;
    private Country country;
    private Object relationships;
}
