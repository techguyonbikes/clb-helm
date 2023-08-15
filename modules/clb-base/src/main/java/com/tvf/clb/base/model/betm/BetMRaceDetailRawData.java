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
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetMRaceDetailRawData {
    @JsonProperty("event_id")
    private Long id;

    @JsonProperty("description")
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String name;

    private String number;

    @JsonProperty("starts_at")
    private Instant startTime;

    private String status;

    private String result;

    private String type;

    private String distance;

    private String state;

    @JsonProperty("meeting_name")
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String meetingName;

    @JsonProperty("additional_comments")
    private String additionalComments;

    @JsonProperty("betting_conditions")
    private String bettingConditions;

    private String condition;

    @JsonProperty("is_betting_allowed")
    private Boolean isBettingAllowed;

    @JsonProperty("is_promoted")
    private Boolean isPromoted;

    private List<BetMRunnerRawData> runners;
}
