package com.tvf.clb.base.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class EntrantRawData {
    private String id;

    private String raceId;
    private String name;
    private int barrier;
    private int number;
    @SerializedName("market_id")
    private String marketId;
    private boolean visible;

    @SerializedName("pricefluctuations")
    private List<Float> priceFluctuations;
    @SerializedName("form_summary")
    private FormSummaryRawData formSummary;
    @SerializedName("is_scratched")
    private boolean isScratched;
    @SerializedName("scratch_time")
    private Instant scratchedTime;
}

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
class FormSummaryRawData {
    private String last20Starts;
    private String riderOrDriver;
    private String trainerName;
    private String bestTime;
    private Float handicapWeight;
    private String entrantComment;
    private Object speedmap;
}
