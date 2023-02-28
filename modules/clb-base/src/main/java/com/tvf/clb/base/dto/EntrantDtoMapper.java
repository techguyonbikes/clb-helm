package com.tvf.clb.base.dto;

import com.tvf.clb.base.model.Entrant;

import java.util.List;

public class EntrantDtoMapper {
    public static EntrantDto toEntrantDto(Entrant entrant, List<Float> prices) {
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
