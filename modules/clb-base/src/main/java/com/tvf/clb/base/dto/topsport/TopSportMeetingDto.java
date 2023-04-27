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
public class TopSportMeetingDto {
    private String id;
    private String name;
    private String country;
    private String state;
    private String raceType;
    private Instant advertisedDate;
    private List<String> raceId;

}
