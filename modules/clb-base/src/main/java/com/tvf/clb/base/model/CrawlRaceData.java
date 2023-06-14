package com.tvf.clb.base.model;

import com.tvf.clb.base.dto.SiteEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class CrawlRaceData {
    private String status;
    private SiteEnum siteEnum;
    private Map<Integer, String> interimResult;
    private Map<Integer, String> finalResult;
    private Map<Integer, CrawlEntrantData> mapEntrants;
    private Instant advertisedStart;
}
