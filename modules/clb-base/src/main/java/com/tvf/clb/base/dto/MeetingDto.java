package com.tvf.clb.base.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Instant advertisedDate;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String categoryId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String venueId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<RaceDto> races;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String trackCondition;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String country;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String state;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean hasFixed;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String regionId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String feedId;

    private String raceType;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> compoundIds;
}
