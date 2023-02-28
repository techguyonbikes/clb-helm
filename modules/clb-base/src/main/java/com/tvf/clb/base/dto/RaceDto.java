package com.tvf.clb.base.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class RaceDto {
    private String id;
    private String meetingId;
    private String name;
    private Integer number;
    private String advertisedStart;
    private String actualStart;
    private List<String> marketIds;
    private String mainMarketStatusId;
    private String resultsDisplay;
}
