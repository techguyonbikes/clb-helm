package com.tvf.clb.base.entity;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table("race_site")
public class RaceSite {
    private Long id;
    private Long generalRaceId;
    private Integer siteId;
    private String raceSiteId;
    private Instant startDate;

}
