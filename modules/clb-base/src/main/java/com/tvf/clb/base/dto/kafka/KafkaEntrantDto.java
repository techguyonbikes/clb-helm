package com.tvf.clb.base.dto.kafka;

import com.tvf.clb.base.model.PriceHistoryData;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class KafkaEntrantDto {
    private Long id;
    private Long raceId;
    private String name;
    private Integer number;
    private Integer barrier;
    private Boolean visible;
    private Boolean isScratched;
    private String scratchedTime;
    private Map<String, List<PriceHistoryData>> winPriceFluctuations;
    private Map<String, List<PriceHistoryData>> placePriceFluctuations;
    private Integer position;
    private String riderOrDriver;
    private String trainerName;
    private String last6Starts;
    private String bestTime;
    private Float handicapWeight;
    private String entrantComment;
    private String bestMileRate;
}
