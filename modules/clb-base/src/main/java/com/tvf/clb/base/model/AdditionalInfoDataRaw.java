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
public class AdditionalInfoDataRaw {
    @SerializedName("distance")
    private Integer distance;
    @SerializedName("generated")
    private Integer generated;
    @SerializedName("distance_type")
    private ConstantModelRaw distanceType;
    @SerializedName("race_comment")
    private String raceComment;
    @SerializedName("silk_base_url")
    private String silkBaseUrl;
    private ConstantModelRaw weather;
    @SerializedName("track_condition")
    private ConstantModelRaw trackCondition;
}