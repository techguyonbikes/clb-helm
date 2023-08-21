package com.tvf.clb.base.model.bluebet;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tvf.clb.base.utils.UpperCaseAndTrimStringDeserializer;
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
@JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
public class BlueBetRaceDetailRawData {
    @JsonAlias("EventId")
    private Long id;

    @JsonProperty("EventName")
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String name;

    @JsonProperty("RaceNumber")
    private Integer number;

    @JsonProperty("RaceDistance")
    private Integer distance;

    private Integer eventTypeId;

    private Integer resultStatusId;

    private Instant advertisedStartTime;

    private Instant bettingCloseTime;

    @JsonProperty("Venue")
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String meetingName;

    private String masterCategoryName;

    @JsonProperty("Outcomes")
    private List<BlueBetEntrantRawData> entrants;

    private BlueBetRaceResultRawData results;

    private List<BlueBetRaceReferenceRawData> raceReferences;
}
