package com.tvf.clb.base.model.zbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
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
public class ZBetRaceRawData {
    private Long id;
    private Integer number;
    private String name;
    private Integer distance;
    private String status;
    private List<Deductions> deductions;
    private List<ZBetEntrantData> selections;

    @JsonProperty("displayed_results")
    private List<ZBetResultsRawData> displayedResults;

    @JsonProperty("result_string")
    private String finalResult;
}


