package com.tvf.clb.base.entity;

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
public class EntrantResponseDto {
    private Long id;
    private String entrantId;
    private Map<Integer, String> raceUUID;
    private Long raceId;
    private String name;
    private Integer number;
    private Integer barrier;
    private Boolean visible;
    private Boolean isScratched;
    private String scratchedTime;
    private Map<Integer, List<Float>> priceFluctuations;
    private Integer position;
}
