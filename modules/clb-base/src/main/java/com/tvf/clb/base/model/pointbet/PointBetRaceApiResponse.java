package com.tvf.clb.base.model.pointbet;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.tvf.clb.base.utils.UpperCaseAndTrimStringDeserializer;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PointBetRaceApiResponse {

    @SerializedName("outcomes")
    List<PointBetEntrantRawData> entrants;
    private String trackCondition;
    private Integer raceDistance;
    @JsonAdapter(UpperCaseAndTrimStringDeserializer.class)
    private String name;
    private String countryCode;
    private Integer racingType;
    private String advertisedStartDateTime;
    public String placing;
}
