package com.tvf.clb.base.model.betright;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tvf.clb.base.utils.UpperCaseAndTrimStringDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetRightMeetingRacesRawData {

    private Integer eventTypeId;
    private String eventId;
    private Integer raceNumber;
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String eventName;
    private Instant advertisedStartTimeUtc;
    private Integer resultStatusId;
    private Boolean isOpenForBetting;
    private Boolean hasFixedMarkets;
    private String results;
}
