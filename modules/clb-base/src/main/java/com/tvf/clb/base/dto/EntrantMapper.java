package com.tvf.clb.base.dto;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.model.EntrantRawData;
import com.tvf.clb.base.model.pointbet.PointBetEntrantRawData;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@NoArgsConstructor(access= AccessLevel.PRIVATE)
public class EntrantMapper {

    public static Gson gson = new Gson();

    public static EntrantDto toEntrantDto(EntrantRawData entrant, List<Float> prices) {
        return EntrantDto.builder()
                .id(entrant.getId())
                .name(entrant.getName())
                .marketId(entrant.getMarketId())
                .number(entrant.getNumber())
                .barrier(entrant.getBarrier())
                .visible(entrant.isVisible())
                .currentSitePrice(prices)
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
                .currentSitePrice(entrant.getCurrentSitePrice())
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

    public static EntrantResponseDto toEntrantResponseDto(Entrant entrant) {
        Gson gson = new Gson();
        Type listType = new TypeToken<Map<Integer, List<Float>>>() {}.getType();
        Map<Integer, List<Float>> prices = gson.fromJson(entrant.getPriceFluctuations().asString(), listType);
        return EntrantResponseDto.builder()
                .id(entrant.getId())
                .entrantId(entrant.getEntrantId())
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
                .currentSitePrice(entrantPrice)
                .position(entrantRawData.getPosition())
                .isScratched(entrantRawData.isScratched())
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
                .currentSitePrice(entrant.getPriceFluctuations())
                .isScratched(entrant.getIsScratched().isEmpty())
                .scratchedTime(entrant.getScratchedTime())
                .position(entrant.getPosition())
                .build();
    }

    public static EntrantRawData toEntrantRawData(RunnerTabRawData runner, List<Integer> position, List<Float> listPrice, String raceId) {
        if (runner.getFixedOdds().getReturnWin() != null) {
            listPrice.add(runner.getFixedOdds().getReturnWin());
        }
        return EntrantRawData.builder()
                .id("TAB-" + runner.getRunnerName() + "-" + runner.getRunnerNumber())
                .raceId(raceId)
                .name(runner.getRunnerName())
                .marketId("")
                .number(runner.getRunnerNumber())
                .barrier(0)
                .visible(false)
                .priceFluctuations(listPrice)
                .isScratched(runner.getFixedOdds().getBettingStatus() == null || !AppConstant.SCRATCHED_NAME.equals(runner.getFixedOdds().getBettingStatus()) ? String.valueOf(false) : String.valueOf(true))
                .scratchedTime(runner.getFixedOdds().getScratchedTime() == null ? null : Instant.parse(runner.getFixedOdds().getScratchedTime()))
                .position(position.indexOf(runner.getRunnerNumber()) + 1)
                .build();
    }

    public static EntrantRawData mapCrawlEntrant(String raceId, ZBetEntrantData entrant, List<Float> prices, Map<Integer, Integer> position) {
        Instant reqInstant = null;
        if (entrant.getScratchingTime() != null) {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(AppConstant.DATE_TIME_PATTERN);
            LocalDateTime localDateTime = LocalDateTime.parse(entrant.getScratchingTime().substring(0, entrant.getScratchingTime().lastIndexOf(".")), dateTimeFormatter);
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
                .isScratched(String.valueOf(entrant.getSelectionsStatus() != null && !AppConstant.NOT_SCRATCHED_NAME.equals(entrant.getSelectionsStatus())))
                .scratchedTime(reqInstant)
                .position(position.get(entrant.getNumber()))
                .build();
    }

    public static List<EntrantResponseDto> convertFromRedisPriceToDTO(List<EntrantResponseDto> dtos) {
        Type listType = new TypeToken<List<EntrantResponseDto>>() {
        }.getType();
        return gson.fromJson(gson.toJson(dtos), listType);
    }
}