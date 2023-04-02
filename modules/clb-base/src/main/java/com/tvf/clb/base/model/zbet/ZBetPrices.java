package com.tvf.clb.base.model.zbet;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ZBetPrices {
    private Long id;
    @SerializedName("win_odds")
    private Float winOdds;
    @SerializedName("place_odds")
    private Float placeOdds;
    @SerializedName("us_place_odds")
    private Float usPlaceOdds;
    @SerializedName("margin_odds")
    private Float marginOdds;
    @SerializedName("top_two_odds")
    private Float topTwoOdds;
    @SerializedName("top_three_odds")
    private Float topThreeOdds;
    @SerializedName("top_four_odds")
    private Float topFourOdds;
    @SerializedName("produce_id")
    private Integer productId;
    @SerializedName("product_code")
    private String productCode;
    @SerializedName("fluctuations")
    private String fluctuations;
    @SerializedName("requested_at")
    private Long requestedAt;
}
