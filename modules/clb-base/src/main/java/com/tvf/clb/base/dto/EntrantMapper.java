package com.tvf.clb.base.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.EntrantResponseDto;
import com.tvf.clb.base.model.EntrantSiteRawData;
import com.tvf.clb.base.model.EntrantRawData;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static EntrantSiteRawData mapPrices(EntrantRawData entrant, Integer siteId, String status) {
        return EntrantSiteRawData.builder()
                .id(entrant.getId())
                .raceId(entrant.getRaceId())
                .name(entrant.getName())
                .marketId(entrant.getMarketId())
                .number(entrant.getNumber())
                .barrier(entrant.getBarrier())
                .visible(entrant.isVisible())
                .priceFluctuations(entrant.getPriceFluctuations() == null ? new ArrayList<>() : entrant.getPriceFluctuations())
                .isScratched(entrant.getIsScratched() == null ? String.valueOf(false) : entrant.getIsScratched())
                .scratchedTime(entrant.getScratchedTime() == null ? Instant.now() : entrant.getScratchedTime())
                .position(entrant.getPosition())
                .siteId(siteId)
                .status(status)
                .build();
    }

    public static EntrantResponseDto toEntrantResponseDto(Entrant entrant, Integer siteId) {
        Map<Integer, List<Double>> priceFluctuations = new HashMap<>();
        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<Double>>() {}.getType();
        ArrayList<Double> prices = gson.fromJson(entrant.getPriceFluctuations().asString(), listType);
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
}