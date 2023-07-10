package com.tvf.clb.base.model.ladbrokes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LadbrokesRaceResult {
    @JsonProperty("market_id")
    private String marketId;
    @JsonProperty("entrant_id")
    private String entrantId;
    @JsonProperty("result_status_id")
    private String resultStatusId;
    private Integer position;
}
