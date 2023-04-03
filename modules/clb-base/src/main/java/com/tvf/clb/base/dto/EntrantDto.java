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
public class EntrantDto {
    private String id;
    private String name;
    private Integer barrier;
    private Integer number;
    private String marketId;
    private Boolean visible;
    private Map<Integer, List<Float>> priceFluctuations;
    private List<Float> currentSitePrice;
    private boolean isScratched;
    private Instant scratchedTime;
    private Integer position;
}
