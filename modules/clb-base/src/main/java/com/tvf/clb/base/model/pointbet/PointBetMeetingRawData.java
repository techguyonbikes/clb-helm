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
public class PointBetMeetingRawData {
    private String masterEventID;
    private String countryCode;
    private String countryName;
    private String firstRaceStartTimeUtc;
    private Integer racingType;
    private String racingTypeName;
    @SerializedName("venue")
    @JsonAdapter(UpperCaseAndTrimStringDeserializer.class)
    private String name;
    private Integer orderById;
    private Boolean hasRaceVision;
    private List<PointBetRacesRawData> races;
    private String version;
    private Boolean hasSameRaceMulti;
}
