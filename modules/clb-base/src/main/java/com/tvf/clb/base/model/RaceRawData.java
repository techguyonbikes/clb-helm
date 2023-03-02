package com.tvf.clb.base.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class RaceRawData {
    private String id;
    private String meetingId;
    private String name;
    private Integer number;
    @SerializedName("advertised_start")
    private String advertisedStart;
    @SerializedName("actual_start")
    private String actualStart;
    @SerializedName("market_ids")
    private List<String> marketIds;
    @SerializedName("main_market_status_id")
    private String mainMarketStatusId;
    @SerializedName("results_display")
    private String resultsDisplay;
}
