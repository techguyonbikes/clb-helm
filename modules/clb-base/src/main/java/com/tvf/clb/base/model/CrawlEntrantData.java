package com.tvf.clb.base.model;

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
public class CrawlEntrantData {
    private Integer position;
    private Boolean isScratched;
    private Instant scratchTime;
    private Map<Integer, List<Float>> priceMap;

    public CrawlEntrantData(Integer position, Map<Integer, List<Float>> priceMap) {
        this.position = position;
        this.priceMap = priceMap;
    }
}
