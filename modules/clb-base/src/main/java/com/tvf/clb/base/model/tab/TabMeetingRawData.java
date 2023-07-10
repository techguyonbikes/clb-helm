package com.tvf.clb.base.model.tab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class TabMeetingRawData {
    @JsonProperty("meetingName")
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String meetingName;
    private String location;
    private String raceType;
    private String meetingDate;
    private String prizeMoney;
    private String weatherCondition;
    private String trackCondition;
    private String railPosition;

    private String venueMnemonic;

    private List<TabRacesData> races;
}
