package com.tvf.clb.base.dto;

import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.model.EntrantRawData;
import io.r2dbc.postgresql.codec.Json;
import reactor.core.publisher.Mono;

import java.util.List;

public class EntrantMapper {
    public static EntrantDto toEntrantDto(EntrantRawData entrant, List<Double> prices) {
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


    public static EntrantRawData mapPrices(EntrantRawData entrant, List<Double> prices) {
        return EntrantRawData.builder()
                .id(entrant.getId())
                .name(entrant.getName())
                .marketId(entrant.getMarketId())
                .number(entrant.getNumber())
                .barrier(entrant.getBarrier())
                .visible(entrant.isVisible())
                .priceFluctuations(prices)
                .build();
    }
    public static Entrant toEntrantExist(Entrant entrant,Entrant entrant1) {
        return Entrant.builder()
                .id(entrant1.getId())
                .name(entrant.getName())
                .marketId(entrant.getMarketId())
                .number(entrant.getNumber())
                .barrier(entrant.getBarrier())
                .visible(entrant.isVisible())
                .priceFluctuations(entrant.getPriceFluctuations())
                .build();
    }
}
