package com.tvf.clb.base.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LadbrokesRaceResult {
    @SerializedName("market_id")
    private String marketId;
    @SerializedName("entrant_id")
    private String entrantId;
    @SerializedName("result_status_id")
    private String resultStatusId;
    private Integer position;
}
