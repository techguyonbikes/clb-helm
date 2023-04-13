package com.tvf.clb.base.dto;

import lombok.*;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceStatusDto {
    private Long id;
    private String status;
}
