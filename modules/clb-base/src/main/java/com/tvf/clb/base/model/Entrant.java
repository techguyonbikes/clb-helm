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
public class Entrant {
    private String id;
    private String name;
    private int barrier;
    private int number;
    @SerializedName("market_id")
    private String marketId;
    private boolean visible;
    @SerializedName("form_summary")
    private FormSummaryDTO formSummary;
}

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
class FormSummaryDTO {
    private String last20Starts;
    private String riderOrDriver;
    private String trainerName;
    private String bestTime;
    private Float handicapWeight;
    private String entrantComment;
    private Object speedmap;
}
