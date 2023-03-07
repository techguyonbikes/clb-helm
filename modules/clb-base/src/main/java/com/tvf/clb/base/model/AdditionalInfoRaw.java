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
public class AdditionalInfoRaw {
    private String raceId;
    private Integer distance;
    @SerializedName("race_comment")
    private String raceComment;
    @SerializedName("distance_type")
    private String  distanceType;
    private Integer generated;
    @SerializedName("silk_base_url")
    private String silkBaseUrl;
    @SerializedName("track_condition")
    private String trackCondition;
    private String weather;
}
