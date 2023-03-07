package com.tvf.clb.base.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ResultsRawData {

    @SerializedName("entrant_id")
    private String entranId;
    @SerializedName("market_id")
    private String marketId;
    private Integer position;
    @SerializedName("result_status_id")
    private String resultStatusId;
}
