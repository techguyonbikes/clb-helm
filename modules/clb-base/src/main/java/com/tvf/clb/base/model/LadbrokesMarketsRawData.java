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
public class LadbrokesMarketsRawData {

    private String id;
    @SerializedName("race_id")
    private String raceId;
    @SerializedName("market_type_id")
    private String marketTypeId;
    @SerializedName("market_status_id")
    private String marketStatusId;
    private String name;
    @SerializedName("entrant_ids")
    private List<String> race_id;
    private Boolean visible;
    @SerializedName("num_winners")
    private Integer numWinners;
}
