package com.tvf.clb.base.model.colossalbet;

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
public class ColBetRaceRawData {
    @JsonProperty("raceId")
    private Long id;

    @JsonProperty("raceNumber")
    private Integer number;

    private String startTime;

    private String status;

    private String result;

    private Float limit;
}
