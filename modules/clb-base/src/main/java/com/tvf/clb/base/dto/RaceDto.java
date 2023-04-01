package com.tvf.clb.base.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class RaceDto {
    private String id;
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

}
