package com.tvf.clb.base.model.zbet;

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
public class ZBetPrices {
    private Long id;
    @JsonProperty("win_odds")
    private Float winOdds;
    @JsonProperty("place_odds")
    private Float placeOdds;
    @JsonProperty("us_place_odds")
    private Float usPlaceOdds;
    @JsonProperty("margin_odds")
    private Float marginOdds;
    @JsonProperty("top_two_odds")
    private Float topTwoOdds;
    @JsonProperty("top_three_odds")
    private Float topThreeOdds;
    @JsonProperty("top_four_odds")
    private Float topFourOdds;
    @JsonProperty("produce_id")
    private Integer productId;
    @JsonProperty("product_code")
    private String productCode;
    @JsonProperty("fluctuations")
    private String fluctuations;
    @JsonProperty("requested_at")
    private Long requestedAt;
}
