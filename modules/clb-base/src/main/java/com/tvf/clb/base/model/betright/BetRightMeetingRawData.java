package com.tvf.clb.base.model.betright;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class BetRightMeetingRawData {

    @JsonProperty("thoroughbred")
    private List<BetRightMeetingRaceTypeRawData> horseMeetingRawData;
    @JsonProperty("greyhounds")
    private List<BetRightMeetingRaceTypeRawData> greyMeetingRawData;
    @JsonProperty("trots")
    private List<BetRightMeetingRaceTypeRawData> harnessMeetingRawData;
}
