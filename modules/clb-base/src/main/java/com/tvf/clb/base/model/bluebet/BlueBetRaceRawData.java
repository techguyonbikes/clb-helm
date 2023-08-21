package com.tvf.clb.base.model.bluebet;

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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
public class BlueBetRaceRawData {
    private Integer eventTypeId;

    @JsonProperty("EventId")
    private Long id;

    @JsonProperty("EventName")
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String name;

    @JsonProperty("RaceNumber")
    private Integer number;

    private Instant advertisedStartTime;

    @JsonProperty("Results")
    private String results;

    private Integer resultStatusId;

    private String stateCode;

    @JsonProperty("Venue")
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String meetingName;

    private String countryCode;

    private Integer venueId;

    private String masterCategoryName;
}
