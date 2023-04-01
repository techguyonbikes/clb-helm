package com.tvf.clb.base.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.EntrantResponseDto;
import com.tvf.clb.base.model.EntrantRawData;
import com.tvf.clb.base.model.tab.RunnerTabRawData;
import com.tvf.clb.base.model.zbet.ZBetEntrantData;
import com.tvf.clb.base.utils.AppConstant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@NoArgsConstructor(access= AccessLevel.PRIVATE)
public class EntrantMapper {

    public static ObjectMapper objectMapper = new ObjectMapper();
    public static Gson gson = new Gson();
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
        Gson gson = new Gson();
        Type listType = new TypeToken<Map<Integer, List<Float>>>() {}.getType();
        Map<Integer, List<Float>> prices = gson.fromJson(entrant.getPriceFluctuations().asString(), listType);
        return EntrantResponseDto.builder()
                .id(entrant.getId())
                .entrantId(entrant.getEntrantId())
                .raceUUID(Collections.singletonMap(siteId, entrant.getRaceUUID()))
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


    public static EntrantResponseDto toEntrantResponseDto(Entrant entrant) {
        Gson gson = new Gson();
        Type listType = new TypeToken<Map<Integer, List<Float>>>() {}.getType();
        Map<Integer, List<Float>> prices = gson.fromJson(entrant.getPriceFluctuations().asString(), listType);
        return EntrantResponseDto.builder()
                .id(entrant.getId())
                .entrantId(entrant.getEntrantId())
//                .raceUUID(entrant.getRaceUUID())
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

    public static EntrantDto toEntrantDto(EntrantRawData entrant) {
        return EntrantDto.builder()
                .id(entrant.getId())
                .name(entrant.getName())
                .marketId(entrant.getMarketId())
                .number(entrant.getNumber())
                .barrier(entrant.getBarrier())
                .visible(entrant.isVisible())
                .priceFluctuations(entrant.getPriceFluctuations())
                .isScratched(entrant.getIsScratched().isEmpty())
                .scratchedTime(entrant.getScratchedTime())
                .position(entrant.getPosition())
                .build();
    }

    public static EntrantRawData toEntrantRawData(RunnerTabRawData runner, List<Integer> position, List<Float> listPrice, String raceId) {

        return EntrantRawData.builder()
                .id("TAB-" + runner.getRunnerName() + "-" + runner.getRunnerNumber())
                .raceId(raceId)
                .name(runner.getRunnerName())
                .marketId("")
                .number(runner.getRunnerNumber())
                .barrier(0)
                .visible(false)
                .priceFluctuations(listPrice)
                .isScratched(runner.getFixedOdds().getBettingStatus().equals(AppConstant.SCRATCHED_NAME) ? String.valueOf(true) : String.valueOf(false))
                .scratchedTime(runner.getFixedOdds().getScratchedTime() == null ? null : Instant.parse(runner.getFixedOdds().getScratchedTime()))
                .position(position.indexOf(runner.getRunnerNumber()) + 1)
                .build();
    }

    public static EntrantRawData mapCrawlEntrant(String raceId, ZBetEntrantData entrant, List<Float> prices, Map<Long, Integer> position) {
        Instant reqInstant = null;
        if (entrant.getScratchingTime() != null) {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime localDateTime = LocalDateTime.parse(entrant.getScratchingTime(), dateTimeFormatter);
            ZoneId zoneId = ZoneId.of(ZoneOffset.UTC.getId());
            reqInstant = localDateTime.atZone(zoneId).toInstant();
        }
        return EntrantRawData.builder()
                .id(entrant.getId().toString())
                .raceId(raceId)
                .name(entrant.getName())
                .marketId(String.valueOf(0))
                .number(entrant.getNumber())
                .barrier(entrant.getBarrier())
                .visible(false)
                .priceFluctuations(prices)
                .isScratched(String.valueOf(entrant.getSelectionsStatus() != null && !entrant.getSelectionsStatus().equals("not scratched")))
                .scratchedTime(reqInstant)
                .position(position.get(entrant.getId()))
                .build();
    }

    public static List<EntrantResponseDto> convertFromRedisPriceToDTO(List<EntrantResponseDto> dtos) {
        Type listType = new TypeToken<List<EntrantResponseDto>>() {
        }.getType();
        return gson.fromJson(gson.toJson(dtos), listType);
    }
}