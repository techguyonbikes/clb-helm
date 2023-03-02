package com.tvf.clb.base.dto;

import com.tvf.clb.base.model.EntrantRawData;

import java.util.List;

public class EntrantMapper {
    public static EntrantDto toEntrantDto(EntrantRawData entrant, List<Float> prices) {
        return EntrantDto.builder()
                .id(entrant.getId())
                .name(entrant.getName())
                .marketId(entrant.getMarketId())
                .number(entrant.getNumber())
                .barrier(entrant.getBarrier())
                .visible(entrant.isVisible())
                .priceFluctuations(prices)
                .build();
    }
}
