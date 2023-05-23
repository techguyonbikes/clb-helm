package com.tvf.clb.base.model.pointbet;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PointBetWinnersRawData {

    private int finalPlacing;
    private String finalPlacingText;
    @SerializedName("outcome")
    private PointBetEntrantRawData entrant;

}
