package com.tvf.clb.base.dto.kafka;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
public class KafkaMeetingDto {
    private Long id;
    private String name;
    private Instant advertisedDate;
    private String trackCondition;
    private String country;
    private String state;
    private Boolean hasFixed;
    private String raceType;
}
