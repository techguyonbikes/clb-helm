package com.tvf.clb.base.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class CrawlEntrantData {

    private Integer position;
    private Integer siteId;
    private Map<Integer, List<Float>> priceMap;
}
