package com.tvf.clb.base.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table("results")
@Slf4j
public class Results {
    @Id
    private Long id;

    private String raceId;
    private String entrantId;
    private String marketId;
    private Integer position;
    private String resultStatusId;

}
