package com.tvf.clb.base.dto;

import com.tvf.clb.base.model.PriceHistoryData;
import lombok.*;
import org.springframework.data.redis.core.RedisHash;
import java.util.List;
import java.util.Map;


@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntrantResponseDto {
    private Long id;
    private String entrantId;
    private String name;
    private Integer number;
    private Integer barrier;
    private Boolean visible;
    private Boolean isScratched;
    private String scratchedTime;
    private Map<Integer, List<PriceHistoryData>> priceFluctuations;
    private Integer position;
    private String riderOrDriver;
    private String trainerName;
}
