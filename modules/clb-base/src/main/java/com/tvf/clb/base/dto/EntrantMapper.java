package com.tvf.clb.base.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.EntrantResponseDto;
import com.tvf.clb.base.model.EntrantRawData;
import com.google.gson.reflect.TypeToken;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor(access= AccessLevel.PRIVATE)
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
                .isScratched(entrant.getIsScratched() != null)
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

    public static EntrantResponseDto toEntrantResponseDto(Entrant entrant, Integer siteId) {
        Map<Integer, List<Float>> priceFluctuations = new HashMap<>();
        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<Float>>() {}.getType();
        ArrayList<Float> prices = gson.fromJson(entrant.getPriceFluctuations().asString(), listType);
        priceFluctuations.put(siteId, prices);
        return EntrantResponseDto.builder()
                .id(entrant.getId())
                .entrantId(entrant.getEntrantId())
                .raceUUID(entrant.getRaceUUID())
                .raceId(entrant.getRaceId())
                .name(entrant.getName())
                .number(entrant.getNumber())
                .barrier(entrant.getBarrier())
                .visible(entrant.isVisible())
                .isScratched(entrant.isScratched())
                .scratchedTime(entrant.isScratched() ? entrant.getScratchedTime().toString() : "")
                .priceFluctuations(priceFluctuations)
                .position(entrant.getPosition())
                .build();
    }


    public static EntrantResponseDto toEntrantResponseDto(Entrant entrant) {
        Gson gson = new Gson();
        Type listType = new TypeToken<Map<Integer, List<Float>>>() {}.getType();
        Map<Integer, List<Float>> prices = gson.fromJson(entrant.getPriceFluctuations().asString(), listType);
        return EntrantResponseDto.builder()
                .id(entrant.getId())
                .entrantId(entrant.getEntrantId())
                .raceUUID(entrant.getRaceUUID())
                .raceId(entrant.getRaceId())
                .name(entrant.getName())
                .number(entrant.getNumber())
                .barrier(entrant.getBarrier())
                .visible(entrant.isVisible())
                .isScratched(entrant.isScratched())
                .scratchedTime(entrant.isScratched() ? entrant.getScratchedTime().toString() : "")
                .priceFluctuations(prices)
                .position(entrant.getPosition())
                .build();
    }
}