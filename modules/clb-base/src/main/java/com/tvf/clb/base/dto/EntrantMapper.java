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
                .name(entrant.getName())
                .marketId(entrant.getMarketId())
                .number(entrant.getNumber())
                .barrier(entrant.getBarrier())
                .visible(entrant.isVisible())
                .priceFluctuations(prices)
                .isScratched(entrant.getIsScratched() ==null ? false :true)
                .scratchedTime(entrant.getScratchedTime())
                .position(entrant.getPosition())
                .build();
    }


    public static EntrantRawData mapPrices(EntrantRawData entrant, List<Float> prices,Integer position) {
        return EntrantRawData.builder()
                .id(entrant.getId())
                .raceId(entrant.getRaceId())
                .name(entrant.getName())
                .marketId(entrant.getMarketId())
                .number(entrant.getNumber())
                .barrier(entrant.getBarrier())
                .visible(entrant.isVisible())
                .priceFluctuations(prices)
                .isScratched(entrant.getIsScratched())
                .scratchedTime(entrant.getScratchedTime())
                .position(position)
                .build();
    }

    public static EntrantResponseDto toEntrantResponseDto (Entrant entrant) {
        return EntrantResponseDto.builder()
                .entrantId(entrant.getEntrantId())
                .entrantName(entrant.getName())
                .priceFluctuations(gson.fromJson(entrant.getPriceFluctuations().asString(), ArrayList.class))
                .build();
    }

    public static EntrantResponseDto toEntrantResponseDto (EntrantDto entrant) {
        return EntrantResponseDto.builder()
                .entrantId(entrant.getId())
                .entrantName(entrant.getName())
                .priceFluctuations(entrant.getPriceFluctuations())
                .build();
    }

    private static final Gson gson = new Gson();
}
