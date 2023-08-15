package com.tvf.clb.base.model.betm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tvf.clb.base.utils.UpperCaseAndTrimStringDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetMRaceRawData {
    @JsonProperty("event_id")
    private Long id;

    @JsonProperty("race_name")
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String name;

    @JsonProperty("race_number")
    private Integer number;

    @JsonProperty("outcome_time")
    private Instant startTime;

    @JsonProperty("race_status")
    private String status;

    private String result;

    @JsonProperty("additional_comments")
    private String additionalComments;

    @JsonProperty("betting_conditions")
    private String bettingConditions;

    private Boolean fo;

    @JsonProperty("is_promoted")
    private Boolean isPromoted;

    @JsonProperty("race_distance")
    private Integer raceDistance;

    @JsonProperty("race_jump_second")
    private Long raceJumpSecond;
}
