package com.tvf.clb.base.model;

import com.google.gson.annotations.SerializedName;
import com.tvf.clb.base.LadbrokesDividendStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LadbrokesRaceDividend {
    private String id;
    @SerializedName("market_id")
    private String marketId;
    private LadbrokesDividendStatus status;
}
