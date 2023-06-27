package com.tvf.clb.base.dto;

import lombok.*;

import java.time.Instant;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceSideBarDto {

    private String status;
    private Instant actualStart;
}