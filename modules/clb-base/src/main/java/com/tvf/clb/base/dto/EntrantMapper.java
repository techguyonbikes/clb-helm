package com.tvf.clb.base.dto;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tvf.clb.base.dto.topsport.TopSportEntrantDto;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.model.CrawlEntrantData;
import com.tvf.clb.base.model.EntrantRawData;
import com.tvf.clb.base.model.PriceHistoryData;
import com.tvf.clb.base.model.colossalbet.ColBetRunnerRawData;
import com.tvf.clb.base.model.playup.PlayUpRunnerRawData;
import com.tvf.clb.base.model.betright.BetRightDeductionsRawData;
import com.tvf.clb.base.model.betright.BetRightEntrantRawData;
import com.tvf.clb.base.model.betright.BetRightWinnersRawData;
import com.tvf.clb.base.model.betm.BetMRunnerRawData;
import com.tvf.clb.base.model.pointbet.PointBetEntrantRawData;
import com.tvf.clb.base.model.sportbet.SportBetEntrantRawData;
import com.tvf.clb.base.model.tab.RunnerTabRawData;
import com.tvf.clb.base.model.tab.TabPriceRawData;
import com.tvf.clb.base.model.zbet.ZBetEntrantData;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.CommonUtils;
import com.tvf.clb.base.utils.ConvertBase;
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

    public static final Gson gson = new Gson();

    public static EntrantDto toEntrantDto(EntrantRawData entrant, List<Float> prices, List<Float> pricePlaces) {
        return EntrantDto.builder()
                .id(entrant.getId())
                .name(entrant.getName())
                .marketId(entrant.getMarketId())
                .number(entrant.getNumber())
                .barrier(entrant.getBarrier())
                .visible(entrant.isVisible())
                .currentSitePrice(prices)
                .currentSitePricePlaces(pricePlaces)
                .isScratched(entrant.getIsScratched() != null)
                .scratchedTime(entrant.getScratchedTime())
                .position(entrant.getPosition())
                .barrierPosition(entrant.getBarrierPosition())
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
                .barrierPosition(entrant.getBarrierPosition())
                .build();
    }

    public static EntrantRawData mapPrices(EntrantRawData entrant, List<Float> prices, List<Float> pricePlaces, Integer position) {
        Float winDeduction = null;
        Float placeDeduction = ConvertBase.getPlaceDeduction(entrant.getDeduction());
        if (entrant.getDeduction() != null) {
            winDeduction = entrant.getDeduction().getWin();
        }
        return EntrantRawData.builder()
                .id(entrant.getId())
                .raceId(entrant.getRaceId())
                .name(entrant.getName())
                .marketId(entrant.getMarketId())
                .number(entrant.getNumber())
                .barrier(entrant.getBarrier())
                .visible(entrant.isVisible())
                .priceFluctuations(prices)
                .pricePlaces(pricePlaces)
                .isScratched(entrant.getIsScratched())
                .scratchedTime(entrant.getScratchedTime())
                .position(position)
                .formSummary(entrant.getFormSummary())
                .barrierPosition(entrant.getBarrierPosition())
                .winDeduction(winDeduction)
                .placeDeduction(placeDeduction)
                .build();
    }

    public static EntrantResponseDto toEntrantResponseDto(Entrant entrant) {
        Gson gson = new Gson();
        Type listType = new TypeToken<Map<Integer, List<PriceHistoryData>>>() {}.getType();
        Type floatType = new TypeToken<Map<Integer, Float>>() {}.getType();
        Map<Integer, List<PriceHistoryData>> prices = entrant.getPriceFluctuations() == null ? new HashMap<>() : gson.fromJson(entrant.getPriceFluctuations().asString(), listType);
        Map<Integer, List<PriceHistoryData>> pricesPlaces = entrant.getPricePlaces() == null ? new HashMap<>() : gson.fromJson(entrant.getPricePlaces().asString(), listType);
        Map<Integer, Float> priceWinDeductions = entrant.getPriceWinDeductions() == null ? new HashMap<>() : gson.fromJson(entrant.getPriceWinDeductions().asString(), floatType);
        Map<Integer, Float> pricePlaceDeductions = entrant.getPricePlaceDeductions() == null ? new HashMap<>() : gson.fromJson(entrant.getPricePlaceDeductions().asString(), floatType);
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
                .pricePlaces(pricesPlaces)
                .winPriceDeductions(priceWinDeductions)
                .placePriceDeductions(pricePlaceDeductions)
                .position(entrant.getPosition())
                .riderOrDriver(entrant.getRiderOrDriver())
                .trainerName(entrant.getTrainerName())
                .last6Starts(entrant.getLast6Starts())
                .handicapWeight(entrant.getHandicapWeight())
                .entrantComment(entrant.getEntrantComment())
                .bestTime(entrant.getBestTime())
                .bestMileRate(entrant.getBestMileRate())
                .barrierPosition(entrant.getBarrierPosition())
                .build();
    }

    public static List<Entrant> toListEntrantEntity(List<PointBetEntrantRawData> entrantRawDataList, Map<String, List<Float>> allEntrantWinPrices, Map<String, List<Float>> allEntrantPlacePrices, String raceUUID) {
        List<Entrant> listEntrantEntity = new ArrayList<>();

        entrantRawDataList.forEach(entrantRawData -> {
            Entrant entrantEntity = toEntrantEntity(entrantRawData, allEntrantWinPrices.getOrDefault(entrantRawData.getId(), new ArrayList<>()), allEntrantPlacePrices.getOrDefault(entrantRawData.getId(), new ArrayList<>()), raceUUID);
            listEntrantEntity.add(entrantEntity);
        });

        return listEntrantEntity;
    }

    public static Entrant toEntrantEntity(PointBetEntrantRawData entrantRawData, List<Float> entrantWinPrice, List<Float> entrantPlacePrice, String raceUUID) {
        return Entrant.builder()
                .entrantId(entrantRawData.getId())
                .raceUUID(raceUUID)
                .name(entrantRawData.getName())
                .number(Integer.parseInt(entrantRawData.getId()))
                .barrier(entrantRawData.getBarrierBox())
                .currentSitePrice(entrantWinPrice)
                .currentSitePricePlaces(entrantPlacePrice)
                .position(entrantRawData.getPosition())
                .isScratched(entrantRawData.isScratched())
                .currentWinDeductions(entrantRawData.getDeduction() == null ? null : entrantRawData.getDeduction().getWin())
                .currentPlaceDeductions(ConvertBase.getPlaceDeduction(entrantRawData.getDeduction()))
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

    public static EntrantRawData toEntrantRawData(RunnerTabRawData runner, List<Integer> position, List<Float> listWinPrice, Float placePrice, String raceId) {
        if (runner == null || raceId == null){
            return new EntrantRawData();
        }
        TabPriceRawData priceRawData = runner.getFixedOdds();
        if (priceRawData.getReturnWin() != null && priceRawData.getReturnWin() != 0) {
            listWinPrice.add(runner.getFixedOdds().getReturnWin());
        }
        return EntrantRawData.builder()
                .id("TAB-" + runner.getRunnerName() + "-" + runner.getRunnerNumber())
                .raceId(raceId)
                .name(runner.getRunnerName())
                .marketId("")
                .number(runner.getRunnerNumber())
                .barrier(0)
                .visible(false)
                .priceFluctuations(listWinPrice)
                .pricePlaces(placePrice == null ? Collections.emptyList() : Collections.singletonList(placePrice))
                .isScratched(runner.getFixedOdds().getBettingStatus() == null || !AppConstant.SCRATCHED_NAME.equals(runner.getFixedOdds().getBettingStatus()) ? String.valueOf(false) : String.valueOf(true))
                .scratchedTime(runner.getFixedOdds().getScratchedTime() == null ? null : Instant.parse(runner.getFixedOdds().getScratchedTime()))
                .position(position.indexOf(runner.getRunnerNumber()) + 1)
                .winDeduction(priceRawData.getWinDeduction() == null ? null : priceRawData.getWinDeduction()/100)
                .placeDeduction(priceRawData.getPlaceDeduction() == null ? null : priceRawData.getPlaceDeduction()/100)
                .build();
    }

    public static EntrantDto toEntrantDto(ZBetEntrantData entrantData, List<Float> pricesFixed, List<Float> pricePlaces){
        return EntrantDto.builder()
                .id(String.valueOf(entrantData.getId()))
                .name(entrantData.getName())
                .barrier(entrantData.getBarrier())
                .number(entrantData.getNumber())
                .currentSitePrice(pricesFixed)
                .currentSitePricePlaces(pricePlaces)
                .currentPlaceDeductions(entrantData.getPlaceDeductions())
                .currentWinDeductions(entrantData.getWinDeductions())
                .build();
    }


    public static EntrantRawData mapCrawlEntrant(String raceId, ZBetEntrantData entrant, List<Float> priceFixes, List<Float> pricesPlaces, Map<Integer, Integer> position) {
        Instant reqInstant = null;
        if (entrant.getScratchingTime() != null) {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(AppConstant.DATE_TIME_PATTERN);
            String dateTimeString = entrant.getScratchingTime();
            if (dateTimeString.contains(".")) {
                dateTimeString = dateTimeString.substring(0, entrant.getScratchingTime().lastIndexOf("."));
            }
            LocalDateTime localDateTime = LocalDateTime.parse(dateTimeString, dateTimeFormatter);
            ZoneId zoneId = ZoneId.of(ZoneOffset.UTC.getId());
            reqInstant = localDateTime.atZone(zoneId).toInstant();
        }

        int entrantPosition = 0;
        if (position != null && position.containsKey(entrant.getNumber())) {
            entrantPosition = position.get(entrant.getNumber());
        }
        return EntrantRawData.builder()
                .id(entrant.getId().toString())
                .raceId(raceId)
                .name(entrant.getName())
                .marketId(String.valueOf(0))
                .number(entrant.getNumber())
                .barrier(entrant.getBarrier())
                .visible(false)
                .priceFluctuations(priceFixes)
                .pricePlaces(pricesPlaces)
                .winDeduction(entrant.getWinDeductions())
                .placeDeduction(entrant.getPlaceDeductions())
                .isScratched(String.valueOf(entrant.getSelectionsStatus() != null && !AppConstant.NOT_SCRATCHED_NAME.equals(entrant.getSelectionsStatus())))
                .scratchedTime(reqInstant)
                .position(entrantPosition)
                .build();
    }

    public static CrawlEntrantData toCrawlEntrantData(EntrantRawData entrant, Integer siteId){
        Map<Integer, List<Float>> priceFluctuations = new HashMap<>();
        priceFluctuations.put(siteId, entrant.getPriceFluctuations());
        Map<Integer, List<Float>> pricePlaces = new HashMap<>();
        pricePlaces.put(siteId, entrant.getPricePlaces());
        Map<Integer, Float> winDeduction = new HashMap<>();
        if (entrant.getWinDeduction() != null) {
            winDeduction.put(siteId, entrant.getWinDeduction());
        }
        Map<Integer, Float> placeDeduction = new HashMap<>();
        if (entrant.getPlaceDeduction() != null) {
            placeDeduction.put(siteId, entrant.getPlaceDeduction());
        }
        return CrawlEntrantData.builder()
                .position(entrant.getPosition())
                .priceMap(priceFluctuations)
                .pricePlacesMap(pricePlaces)
                .winDeductions(winDeduction)
                .placeDeductions(placeDeduction)
                .isScratched(entrant.getIsScratched() == null ? Boolean.FALSE : Boolean.parseBoolean(entrant.getIsScratched()))
                .scratchTime(entrant.getScratchedTime())
                .build();
    }
    public static Entrant toEntrantEntity(TopSportEntrantDto entrantRawData) {
        return Entrant.builder()
                .entrantId("TOPSPORT" + entrantRawData.getEntrantName() + entrantRawData.getBarrier())
                .raceUUID(entrantRawData.getRaceUUID())
                .number(entrantRawData.getNumber())
                .name(entrantRawData.getEntrantName())
                .isScratched(entrantRawData.isScratched())
                .currentSitePrice(entrantRawData.getPriceWins())
                .currentSitePricePlaces(entrantRawData.getPricePlaces())
                .scratchedTime(entrantRawData.getScratchedTime())
                .currentWinDeductions(entrantRawData.getPriceWinScratch())
                .currentPlaceDeductions(entrantRawData.getPricePlacesScratch())
                .build();
    }

    public static EntrantDto toEntrantDto(SportBetEntrantRawData entrant) {
        return EntrantDto.builder()
                .id(entrant.getId().toString())
                .name(entrant.getName())
                .number(entrant.getRunnerNumber())
                .build();
    }

    public static List<Entrant> toListEntrantEntityBetRight(List<BetRightEntrantRawData> entrantRawDataList, Map<Integer, List<Float>> allEntrantWinPrices,
                                                            Map<Integer, List<Float>> allEntrantPlacePrices, String raceUUID, Map<Integer, BetRightDeductionsRawData> allEntrantDeductions,
                                                            Map<Integer, BetRightWinnersRawData> allEntrantWinners) {
        List<Entrant> listEntrantEntity = new ArrayList<>();

        entrantRawDataList.forEach(entrantRawData -> {
            Integer entrantID = entrantRawData.getOutcomeId();
            Integer position = CommonUtils.applyIfNotEmpty(allEntrantWinners.get(entrantID), BetRightWinnersRawData::getFinalPlacing);
            Float deductionWin = CommonUtils.applyIfNotEmpty(allEntrantDeductions.get(entrantID), BetRightDeductionsRawData::getDeductionWin);
            Float deductionPlace = CommonUtils.applyIfNotEmpty(allEntrantDeductions.get(entrantID), BetRightDeductionsRawData::getDeductionPlace);
            Entrant entrantEntity = toEntrantEntityBetRight(entrantRawData, allEntrantWinPrices.getOrDefault(entrantID, new ArrayList<>()),
                    allEntrantPlacePrices.getOrDefault(entrantID, new ArrayList<>()), raceUUID, position, deductionWin, deductionPlace);
            listEntrantEntity.add(entrantEntity);
        });

        return listEntrantEntity;
    }

    public static Entrant toEntrantEntityBetRight(BetRightEntrantRawData entrantRawData, List<Float> entrantWinPrice, List<Float> entrantPlacePrice,
                                                  String raceUUID, Integer position, Float deductionWin, Float deductionPlace) {
        return Entrant.builder()
                .entrantId(String.valueOf(entrantRawData.getOutcomeId()))
                .raceUUID(raceUUID)
                .name(entrantRawData.getOutcomeName())
                .number(entrantRawData.getOutcomeId())
                .barrier(entrantRawData.getBarrierBox())
                .currentSitePrice(entrantWinPrice)
                .currentSitePricePlaces(entrantPlacePrice)
                .isScratched(entrantRawData.getScratched())
                .position(position)
                .currentWinDeductions(deductionWin)
                .currentPlaceDeductions(deductionPlace)
                .build();
    }
    public static Entrant toEntrantEntityPlayUp(PlayUpRunnerRawData entrantRawData,List<Float> entrantWinPrice, List<Float> entrantPlacePrice, String raceUUID) {
        return Entrant.builder()
                .entrantId(entrantRawData.getId())
                .raceUUID(raceUUID)
                .number(entrantRawData.getNumber())
                .name(entrantRawData.getName())
                .currentSitePrice(entrantWinPrice)
                .currentSitePricePlaces(entrantPlacePrice)
                .currentWinDeductions(entrantRawData.getDeductions() == null ? null: entrantRawData.getDeductions().getWin())
                .currentWinDeductions(entrantRawData.getDeductions() == null ? null: entrantRawData.getDeductions().getPlace())
                .build();
    }
    public static EntrantDto toEntrantDto(PlayUpRunnerRawData entrant) {
        return EntrantDto.builder()
                .id(entrant.getId())
                .name(entrant.getName())
                .number(entrant.getNumber())
                .build();
    }

    public static Entrant toEntrantEntity(BetMRunnerRawData entrant) {
        boolean isScratched = Boolean.TRUE.equals(entrant.getIsScratched());
        Float winDeduction = isScratched ? entrant.getWinDeduction() : null;
        Float placeDeduction = isScratched ? entrant.getPlaceDeduction() : null;

        return Entrant.builder()
                .name(entrant.getName())
                .number(entrant.getNumber())
                .isScratched(isScratched)
                .scratchedTime(entrant.getScratchedAt())
                .currentSitePrice(entrant.getWinPrice() == null ? Collections.emptyList() : Collections.singletonList(entrant.getWinPrice()))
                .currentSitePricePlaces(entrant.getPlacePrice() == null ? Collections.emptyList() : Collections.singletonList(entrant.getPlacePrice()))
                .currentWinDeductions(winDeduction)
                .currentPlaceDeductions(placeDeduction)
                .build();
    }

    public static CrawlEntrantData toCrawlEntrantData(BetMRunnerRawData entrantRawData) {
        Map<Integer, List<Float>> winPrices = new HashMap<>();
        winPrices.put(AppConstant.BET_M_SITE_ID, entrantRawData.getWinPrice() == null ? Collections.emptyList() : Collections.singletonList(entrantRawData.getWinPrice()));

        Map<Integer, List<Float>> placePrices = new HashMap<>();
        placePrices.put(AppConstant.BET_M_SITE_ID, entrantRawData.getPlacePrice() == null ? Collections.emptyList() : Collections.singletonList(entrantRawData.getPlacePrice()));

        Map<Integer, Float> winDeduction = new HashMap<>();
        Map<Integer, Float> placeDeduction = new HashMap<>();
        if (Boolean.TRUE.equals(entrantRawData.getIsScratched())) {
            if (entrantRawData.getWinDeduction() != null) {
                winDeduction.put(AppConstant.BET_M_SITE_ID, entrantRawData.getWinDeduction());
            }
            if (entrantRawData.getPlaceDeduction() != null) {
                placeDeduction.put(AppConstant.BET_M_SITE_ID, entrantRawData.getPlaceDeduction());
            }
        }

        return new CrawlEntrantData(0, winPrices, placePrices, winDeduction, placeDeduction);
    }
    public static CrawlEntrantData toCrawlEntrantData(ColBetRunnerRawData entrantRawData) {
        Map<Integer, List<Float>> winPrices = new HashMap<>();
        winPrices.put(AppConstant.COLOSSAL_BET_SITE_ID, entrantRawData.getWinPrice() == null ? Collections.emptyList() : Collections.singletonList(entrantRawData.getWinPrice()));

        Map<Integer, List<Float>> placePrices = new HashMap<>();
        placePrices.put(AppConstant.COLOSSAL_BET_SITE_ID, entrantRawData.getPlacePrice() == null ? Collections.emptyList() : Collections.singletonList(entrantRawData.getPlacePrice()));

        Map<Integer, Float> winDeduction = new HashMap<>();
        Map<Integer, Float> placeDeduction = new HashMap<>();
        if (Boolean.TRUE.equals(entrantRawData.getIsScratched())) {
            if (entrantRawData.getWinDeduction() != null) {
                winDeduction.put(AppConstant.COLOSSAL_BET_SITE_ID, entrantRawData.getWinDeduction());
            }
            if (entrantRawData.getPlaceDeduction() != null) {
                placeDeduction.put(AppConstant.COLOSSAL_BET_SITE_ID, entrantRawData.getPlaceDeduction());
            }
        }

        return new CrawlEntrantData(0, winPrices, placePrices, winDeduction, placeDeduction);
    }
    public static Entrant toEntrantEntity(ColBetRunnerRawData entrant) {
        boolean isScratched = Boolean.TRUE.equals(entrant.getIsScratched());
        Float winDeduction = isScratched ? entrant.getWinDeduction() : null;
        Float placeDeduction = isScratched ? entrant.getPlaceDeduction() : null;

        return Entrant.builder()
                .name(entrant.getName())
                .number(entrant.getNumber())
                .isScratched(isScratched)
                /*.scratchedTime(entrant.getScratchedAt())*/
                .currentSitePrice(entrant.getWinPrice() == null ? Collections.emptyList() : Collections.singletonList(entrant.getWinPrice()))
                .currentSitePricePlaces(entrant.getPlacePrice() == null ? Collections.emptyList() : Collections.singletonList(entrant.getPlacePrice()))
                .currentWinDeductions(winDeduction)
                .currentPlaceDeductions(placeDeduction)
                .build();
    }
}