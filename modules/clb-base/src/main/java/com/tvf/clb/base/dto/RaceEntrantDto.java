package com.tvf.clb.base.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class RaceEntrantDto {

    private Long id;
    private Long meetingId;
    private String meetingName;
    private String raceType;
    private String state;
    private Instant advertisedStart;
    private Instant actualStart;
    private String name;
    private Integer number;
    private String distance;
    private String status;
    private String country;
    private String silkUrl;
    private String fullFormUrl;
    private Map<Integer, String> finalResult;
    private Map<Integer, Long> raceIdNumber;
    private Map<Integer, String> raceSiteUUID;
    private List<EntrantResponseDto> entrants;
    private Map<Integer, String> raceSiteUrl;
}
