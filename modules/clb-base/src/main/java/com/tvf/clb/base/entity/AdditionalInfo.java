package com.tvf.clb.base.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table("additionalinfo")
@Slf4j
public class AdditionalInfo {
    @Id
    private long id;
    private String raceId;
    private Integer distance;
    private String raceComment;
    private String  distanceType;
    private Integer generated;
    private String silkBaseUrl;
    private String trackCondition;
    private String weather;




}
