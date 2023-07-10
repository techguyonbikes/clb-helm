package com.tvf.clb.base.model.ladbrokes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LadbrokesMarketsRawData {

    private String id;
    @JsonProperty("race_id")
    private String raceId;
    @JsonProperty("market_type_id")
    private String marketTypeId;
    @JsonProperty("market_status_id")
    private String marketStatusId;
    private String name;
    @JsonProperty("entrant_ids")
    private List<String> entrantIds;
    private Boolean visible;
    @JsonProperty("num_winners")
    private Integer numWinners;
}
