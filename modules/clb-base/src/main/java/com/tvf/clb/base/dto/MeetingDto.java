package com.tvf.clb.base.dto;

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
public class MeetingDto {
    private String id;
    private String name;
    private Instant advertisedDate;
    private String categoryId;
    private String venueId;
    private List<RaceDto> races;
    private String trackCondition;
    private String country;
    private String state;
    private Boolean hasFixed;
    private String regionId;
    private String feedId;
    private String raceType;
    private List<String> compoundIds;
}
