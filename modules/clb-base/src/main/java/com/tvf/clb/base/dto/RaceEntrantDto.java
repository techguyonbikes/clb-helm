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
public class RaceEntrantDto {

    private Long id;
    private String meetingId;
    private String name;
    private Integer number;
    private String distance;
    private String status;
    private List<EntrantResponseDto> entrants;
}
