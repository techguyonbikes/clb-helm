package com.tvf.clb.base.dto;

import com.tvf.clb.base.model.PriceHistoryData;
import lombok.*;
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
    private Map<Integer, List<PriceHistoryData>> pricePlaces;
    private Integer position;
    private String riderOrDriver;
    private String trainerName;
    private String last6Starts;
    private String bestTime;
    private Float handicapWeight;
    private String entrantComment;
    private String bestMileRate;
    private String barrierPosition;
}
