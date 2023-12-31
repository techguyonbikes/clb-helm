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
    private Map<Integer, List<Float>> pricePlacesMap;
    private Map<Integer, Float> winDeductions;
    private Map<Integer, Float> placeDeductions;

    public CrawlEntrantData(Integer position, Map<Integer, List<Float>> priceMap, Map<Integer, List<Float>> pricePlacesMap, Map<Integer, Float> winDeductions, Map<Integer, Float> placeDeductions) {
        this.position = position;
        this.priceMap = priceMap;
        this.pricePlacesMap = pricePlacesMap;
        this.winDeductions = winDeductions;
        this.placeDeductions = placeDeductions;
    }
}
