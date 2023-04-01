package com.tvf.clb.base.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.EntrantResponseDto;
import com.tvf.clb.base.model.EntrantRawData;
import com.tvf.clb.base.model.pointbet.PointBetEntrantRawData;
import com.tvf.clb.base.model.tab.RunnerTabRawData;
import com.tvf.clb.base.utils.AppConstant;
import io.r2dbc.postgresql.codec.Json;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Type;
import java.time.Instant;
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

    public static EntrantDto toEntrantDto(Entrant entrant) {
        return EntrantDto.builder()
                .id(entrant.getEntrantId())
                .name(entrant.getName())
                .marketId(entrant.getMarketId())
                .number(entrant.getNumber())
                .barrier(entrant.getBarrier())
                .visible(entrant.isVisible())
                .priceFluctuations(entrant.getPrices())
                .isScratched(entrant.isScratched())
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

    public static List<Entrant> toListEntrantEntity(List<PointBetEntrantRawData> entrantRawDataList, Map<String, List<Float>> allEntrantPrices, String raceUUID) {
        List<Entrant> listEntrantEntity = new ArrayList<>();

        entrantRawDataList.forEach(entrantRawData -> {
            Entrant entrantEntity = toEntrantEntity(entrantRawData, allEntrantPrices.getOrDefault(entrantRawData.getId(), new ArrayList<>()), raceUUID);
            listEntrantEntity.add(entrantEntity);
        });

        return listEntrantEntity;
    }

    public static Entrant toEntrantEntity(PointBetEntrantRawData entrantRawData, List<Float> entrantPrice, String raceUUID) {
        return Entrant.builder()
                .entrantId(entrantRawData.getId())
                .raceUUID(raceUUID)
                .name(entrantRawData.getName())
                .number(Integer.parseInt(entrantRawData.getId()))
                .barrier(entrantRawData.getBarrierBox())
                .priceFluctuations(Json.of(new Gson().toJson(entrantPrice)))
                .position(entrantRawData.getPosition())
                .isScratched(entrantRawData.isScratched())
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

    public static List<EntrantResponseDto> convertFromRedisPriceToDTO(List<EntrantResponseDto> dtos) {
        Type listType = new TypeToken<List<EntrantResponseDto>>() {
        }.getType();
        return gson.fromJson(gson.toJson(dtos), listType);
    }
}