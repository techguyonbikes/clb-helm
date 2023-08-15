package com.tvf.clb.base.model.betm;

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
public class BetMMeetingRawData {
    @JsonProperty("display_order_priority")
    private Integer displayOrderPriority;

    @JsonProperty("meeting_class")
    private String meetingClass;

    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    @JsonProperty("meeting_name")
    private String name;

    @JsonProperty("race_state")
    private String raceState;

    @JsonProperty("race_type")
    private String raceType;

    @JsonProperty("vic_code")
    private String vicCode;

    private List<BetMRaceRawData> races;
}
