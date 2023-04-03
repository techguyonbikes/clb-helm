package com.tvf.clb.base.model.tab;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.tvf.clb.base.utils.UpperCaseAndTrimStringDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TabMeetingRawData {
    @SerializedName("meetingName")
    @JsonAdapter(UpperCaseAndTrimStringDeserializer.class)
    private String meetingName;
    private String location;
    private String raceType;
    private String meetingDate;
    private String prizeMoney;
    private String weatherCondition;
    private String trackCondition;
    private Boolean railPosition;

    private String venueMnemonic;

    private List<TabRacesData> races;
}
