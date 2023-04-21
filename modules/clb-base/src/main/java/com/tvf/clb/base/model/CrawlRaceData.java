package com.tvf.clb.base.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class CrawlRaceData {
    private String status;
    private Integer siteId;
    private Map<Integer, String> finalResult;
    private Map<Integer, CrawlEntrantData> mapEntrants;
}
