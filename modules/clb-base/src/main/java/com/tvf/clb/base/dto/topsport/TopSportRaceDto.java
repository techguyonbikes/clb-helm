package com.tvf.clb.base.dto.topsport;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class TopSportRaceDto {
    private String id;
    private String raceName;
    private String meetingName;
    private List<TopSportEntrantDto>  runners;
    private String results;
    private int raceNumber;
    private int distance;
    private Instant startTime;
    private String raceType;

}
