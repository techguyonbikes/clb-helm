package com.tvf.clb.base.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class RaceDto {
    private String id;
    private Long raceId;
    private Long meetingId;
    private String meetingUUID;
    private String meetingName;
    private String name;
    private Integer number;
    private String raceType;
    private Instant advertisedStart;
    private Instant actualStart;
    private List<String> marketIds;
    private String mainMarketStatusId;
    private String resultsDisplay;
    private Integer distance;
    private String status;
    private String finalResult;
    private String raceSiteUrl;
    private String silkUrl;
    private String fullFormUrl;
    private String countryCode;
    private String state;
    private String venueId;
}
