package com.tvf.clb.base.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.model.EntrantRawData;
import io.r2dbc.postgresql.codec.Json;
import reactor.core.publisher.Mono;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class EntrantMapper {

    public static ObjectMapper objectMapper = new ObjectMapper();
    public static EntrantDto toEntrantDto(EntrantRawData entrant, List<Float> prices) {
        return EntrantDto.builder()
                .id(entrant.getId())
                .raceId(entrant.getRaceId())
                .name(entrant.getName())
                .marketId(entrant.getMarketId())
                .number(entrant.getNumber())
                .barrier(entrant.getBarrier())
                .visible(entrant.isVisible())
                .priceFluctuations(prices)
                .build();
    }


    public static EntrantRawData mapPrices(EntrantRawData entrant, List<Float> prices) {
        return EntrantRawData.builder()
                .id(entrant.getId())
                .raceId(entrant.getRaceId())
                .name(entrant.getName())
                .marketId(entrant.getMarketId())
                .number(entrant.getNumber())
                .barrier(entrant.getBarrier())
                .visible(entrant.isVisible())
                .priceFluctuations(prices)
                .build();
    }

    public static EntrantResponseDto toEntrantResponseDto (Entrant entrant) {
        return EntrantResponseDto.builder()
                .entrantId(entrant.getEntrantId())
                .priceFluctuations(entrant.getPriceFluctuations().toString())
                .build();
    }
}
