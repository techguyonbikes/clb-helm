package com.tvf.clb.base.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.model.EntrantSiteRawData;
import com.tvf.clb.base.model.EntrantRawData;

import java.time.Instant;
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

    public static EntrantSiteRawData mapPrices(EntrantRawData entrant, int siteId, String status) {
        return EntrantSiteRawData.builder()
                .id(entrant.getId())
                .raceId(entrant.getRaceId())
                .name(entrant.getName())
                .marketId(entrant.getMarketId())
                .number(entrant.getNumber())
                .barrier(entrant.getBarrier())
                .visible(entrant.isVisible())
                .priceFluctuations(entrant.getPriceFluctuations())
                .isScratched(entrant.getIsScratched() == null ? "titus" : entrant.getIsScratched())
                .scratchedTime(entrant.getScratchedTime() == null ? Instant.now() : entrant.getScratchedTime())
                .position(entrant.getPosition())
                .siteId(siteId)
                .status(status)
                .build();
    }
}
