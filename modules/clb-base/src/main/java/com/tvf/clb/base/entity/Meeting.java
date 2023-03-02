package com.tvf.clb.base.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table("meeting")
public class Meeting {
    @Id
    private Long id;
    private String meetingId;
    private String name;
    private Instant advertisedDate;
    private String categoryId;
    private String venueId;
    private String trackCondition;
    private String country;
    private String state;
    private Boolean hasFixed;
    private String regionId;
    private String feedId;
    private String raceType;
}
