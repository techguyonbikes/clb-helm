package com.tvf.clb.base.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tvf.clb.base.utils.UpperCaseAndTrimStringDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Optional;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RaceRawData {
    private String id;
    @JsonProperty("meeting_id")
    private String meetingId;
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String name;
    private Integer number;
    @JsonProperty("advertised_start")
    private String advertisedStart;
    @JsonProperty("actual_start")
    private String actualStart;
    @JsonProperty("market_ids")
    private List<String> marketIds;
    @JsonProperty("main_market_status_id")
    private String mainMarketStatusId;
    @JsonProperty("results_display")
    private String resultsDisplay;

    private Integer distance;

    public String getName() {
        return Optional.ofNullable(name).orElse("RACE " + number);
    }
}


