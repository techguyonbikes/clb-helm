package com.tvf.clb.base.dto;

import lombok.*;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RaceFinalResultDto {
    private Long id;
    private Map<Integer, String> result;
}
