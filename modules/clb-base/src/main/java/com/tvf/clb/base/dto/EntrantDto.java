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
public class EntrantDto {
    private String id;
    private String name;
    private Integer barrier;
    private Integer number;
    private String marketId;
    private Boolean visible;
    private List<Float> priceFluctuations;
}
