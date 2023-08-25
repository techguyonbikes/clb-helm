package com.tvf.clb.base.model.betright;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetRightRaceRawData {

    private String eventId;
    private String eventName;
    @JsonProperty("outcomes")
    private List<BetRightEntrantRawData> entrantRawData;
    private Instant advertisedStartTimeUtc;
    private String trackCondition;
    private String weatherCondition;
    private String venue;
    private Integer raceNumber;
    private Integer raceDistance;
    private String masterEventId;
    private Integer eventTypeId;
    private Integer resultStatusId;
    private BetRightResultsRawData results;
    private String countryCode;
}
