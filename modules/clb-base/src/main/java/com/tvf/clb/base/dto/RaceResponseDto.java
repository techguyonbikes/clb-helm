package com.tvf.clb.base.dto;

import lombok.*;
import org.springframework.data.redis.core.RedisHash;

import java.util.List;
import java.util.Map;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("race")
public class RaceResponseDto {
    private Long id;
    private String raceName;
    private Integer raceNumber;
    private String raceType;
    private Integer distance;
    private String status;
    private String advertisedStart;
    private String actualStart;
    private String silkUrl;
    private String fullFormUrl;
    private String venueId;
    private Map<Integer, String> mapSiteUUID;
    private Map<Integer, String> finalResult;
    private Map<Integer, String> interimResult;
    private Map<Integer, String> raceSiteUrl;
    private List<EntrantResponseDto> entrants;
}
