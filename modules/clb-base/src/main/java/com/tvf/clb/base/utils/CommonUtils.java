package com.tvf.clb.base.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tvf.clb.base.dto.EntrantMapper;
import com.tvf.clb.base.dto.RaceDto;
import com.tvf.clb.base.dto.RaceResponseDto;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.model.EntrantRawData;
import com.tvf.clb.base.model.PriceHistoryData;
import com.tvf.clb.base.model.ladbrokes.LadBrokedItRaceDto;
import com.tvf.clb.base.model.ladbrokes.LadBrokesPriceOdds;
import com.tvf.clb.base.model.ladbrokes.LadbrokesMarketsRawData;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.tvf.clb.base.utils.AppConstant.*;


public class CommonUtils {

    public static Map<Integer, List<PriceHistoryData>> getSitePriceFromJsonb(Json json) {
        if (json == null) {
            return new HashMap<>();
        }
        Type type = new TypeToken<Map<Integer, List<PriceHistoryData>>>() {}.getType();
        return new Gson().fromJson(json.asString(), type);
    }

    // "NSW", "SA", "VIC", "QLD", "NT", "TAS", "WA","NZL"
    public static String checkDiffStateMeeting(String state) {
        if (AppConstant.CODE_NZL.equals(state)) {
            return AppConstant.CODE_NZ ;
        }
        return state;
    }

    public static Map<Integer, String> getMapRaceFinalResultFromJsonb(Json json) {
        if (json == null) {
            return new HashMap<>();
        }
        Type type = new TypeToken<Map<Integer, String>>() {}.getType();
        return new Gson().fromJson(json.asString(), type);
    }

    public static boolean isRaceFinalOrAbandonedInAllSite(RaceResponseDto race) {
        String raceStatus = race.getStatus();

        if (race.getStatus() == null) {
            return false;
        } else if (AppConstant.STATUS_ABANDONED.equals(raceStatus)) {
            return true;
        } else if (AppConstant.STATUS_FINAL.equals(raceStatus) || AppConstant.STATUS_RE_RESULTED.equals(raceStatus)) {
            // check all site have final result
            return race.getFinalResult() != null && race.getFinalResult().size() == race.getMapSiteUUID().size();
        }

        return false;
    }

    public static Race mapNewRaceToExisting(Map<Race, List<Entrant>> mapExistingRaceAndEntrants, RaceDto newRace, List<Entrant> newEntrants) {
        if (mapExistingRaceAndEntrants.isEmpty()) {
            return null;
        }

        List<Race> mostSimilarRacesByEntrant = getTheMostSimilarRacesByEntrant(mapExistingRaceAndEntrants, newEntrants);

        if (mostSimilarRacesByEntrant.size() == 1) {
            return mostSimilarRacesByEntrant.get(0);
        } else {
            return getMostSimilarRaceByMeetingName(mostSimilarRacesByEntrant, newRace);
        }
    }

    private static List<Race> getTheMostSimilarRacesByName(Map<Race, List<Entrant>> mapExistingRaceAndEntrants, RaceDto newRace) {
        List<Race> mostSimilarRaces = new ArrayList<>();
        int maxSameWords = 0;

        for (Race existingRace: mapExistingRaceAndEntrants.keySet()) {
            int numberOfSameWords = compareName(existingRace.getName(), newRace.getName());

            if (numberOfSameWords > maxSameWords) {
                mostSimilarRaces = new ArrayList<>();
                mostSimilarRaces.add(existingRace);
                maxSameWords = numberOfSameWords;
            } else if (numberOfSameWords > 0 && numberOfSameWords == maxSameWords) {
                mostSimilarRaces.add(existingRace);
            }
        }

        return mostSimilarRaces;
    }

    private static List<Race> getTheMostSimilarRacesByEntrant(Map<Race, List<Entrant>> mapExistingRaceAndEntrants, List<Entrant> newEntrants) {
        if (CollectionUtils.isEmpty(newEntrants)) {
            return new ArrayList<>();
        }

        List<Race> mostSimilarRaces = new ArrayList<>();
        int maxSameEntrants = 0;

        for (Map.Entry<Race, List<Entrant>> entry : mapExistingRaceAndEntrants.entrySet()) {
            Race existingRace = entry.getKey();
            List<Entrant> existingEntrants = entry.getValue();

            if (existingEntrants != null && existingEntrants.size() == newEntrants.size()) {
                int numberOfSameEntrants = compareEntrants(existingEntrants, newEntrants);
                if (numberOfSameEntrants > newEntrants.size() / 2) {
                    if (numberOfSameEntrants > maxSameEntrants) {
                        mostSimilarRaces = new ArrayList<>();
                        mostSimilarRaces.add(existingRace);
                        maxSameEntrants = numberOfSameEntrants;
                    } else if (numberOfSameEntrants == maxSameEntrants) {
                        mostSimilarRaces.add(existingRace);
                    }
                }
            }
        }

        return mostSimilarRaces;
    }

    private static Race getMostSimilarRaceByMeetingName(List<Race> listRace, RaceDto newRace) {
        Race result = null;
        int maxSameMeetingName = 0;
        for (Race race : listRace) {
            int numberOfSameMeetingName = compareName(race.getMeetingName(), newRace.getMeetingName());
            if (numberOfSameMeetingName > maxSameMeetingName) {
                maxSameMeetingName = numberOfSameMeetingName;
                result = race;
            }
        }
        return result;
    }

    public static int compareEntrants(List<Entrant> targetEntrants, List<Entrant> entrantsNeedToCheck) {
        int numberOfSameEntrants = 0;
        Map<Integer, Entrant> mapNumberAndTargetEntrant = targetEntrants.stream().collect(Collectors.toMap(Entrant::getNumber, Function.identity()));
        Map<Integer, Entrant> mapNumberAndEntrantNeedToCheck = entrantsNeedToCheck.stream().collect(Collectors.toMap(Entrant::getNumber, Function.identity()));

        for (Map.Entry<Integer, Entrant> entry : mapNumberAndTargetEntrant.entrySet()) {
            Integer entrantNumber = entry.getKey();
            Entrant targetEntrant = entry.getValue();

            if (mapNumberAndEntrantNeedToCheck.containsKey(entrantNumber)) {
                Entrant entrantNeedToCheck = mapNumberAndEntrantNeedToCheck.get(entrantNumber);
                if (targetEntrant.getName().contains(entrantNeedToCheck.getName()) || entrantNeedToCheck.getName().contains(targetEntrant.getName())) {
                    numberOfSameEntrants++;
                }
            }
        }
        return numberOfSameEntrants;
    }

    public static Meeting mapNewMeetingToExisting(Map<Meeting, List<Race>> mapExisingMeetingAndRace, Meeting meetingNeedToCheck, List<Race> racesNeedToCheck) {
        if (mapExisingMeetingAndRace.isEmpty() || meetingNeedToCheck == null) {
            return null;
        }

        List<Meeting> mostCommonMeetings = new ArrayList<>();
        int maxCommonWords = 0;

        for (Meeting existing : mapExisingMeetingAndRace.keySet()) {
            int numberOfCommonWord = compareName(existing.getName(), meetingNeedToCheck.getName());

            if (numberOfCommonWord > maxCommonWords) {
                maxCommonWords = numberOfCommonWord;
                mostCommonMeetings = new ArrayList<>();
                mostCommonMeetings.add(existing);
            } else if (numberOfCommonWord > 0 && numberOfCommonWord == maxCommonWords) {
                mostCommonMeetings.add(existing);
            }
        }

        if (mostCommonMeetings.size() == 1 && mostCommonMeetings.get(0).getName().equals(meetingNeedToCheck.getName())) {
            return mostCommonMeetings.get(0);
        } else {
            Meeting result = null;
            int maxCommonRace = 0;
            for (Meeting meeting : mostCommonMeetings) {
                int numberOfCommonRaces = compareRaces(mapExisingMeetingAndRace.get(meeting), racesNeedToCheck);
                if (numberOfCommonRaces > maxCommonRace) {
                    maxCommonRace = numberOfCommonRaces;
                    result = meeting;
                }
            }
            return result;
        }
    }

    public static int compareRaces(List<Race> targetRaces, List<Race> listRaceNeedToCheck) {
        int numberOfCommonRaces = 0;
        Map<Integer, Race> mapNumberAndTargetRace = targetRaces.stream().collect(Collectors.toMap(Race::getNumber, Function.identity()));
        Map<Integer, Race> mapNumberAndRaceNeedToCheck = listRaceNeedToCheck.stream().collect(Collectors.toMap(Race::getNumber, Function.identity()));

        for (Map.Entry<Integer, Race> entry : mapNumberAndTargetRace.entrySet()) {
            Integer number = entry.getKey();
            Race targetRace = entry.getValue();
            if (mapNumberAndRaceNeedToCheck.containsKey(number)) {
                Race raceNeedToCheck = mapNumberAndRaceNeedToCheck.get(number);
                if ((targetRace.getName().contains(raceNeedToCheck.getName()) || raceNeedToCheck.getName().contains(targetRace.getName()))
                        && targetRace.getAdvertisedStart().equals(raceNeedToCheck.getAdvertisedStart())) {
                    numberOfCommonRaces++;
                }
            }
        }
        return numberOfCommonRaces;
    }

    public static int compareName(String meetingName1, String meetingName2) {
        if (meetingName1 == null || meetingName2 == null) {
            return 0;
        }
        meetingName1 = meetingName1.toLowerCase();
        meetingName2 = meetingName2.toLowerCase();

        String[] words1 = meetingName1.split("\\s+");
        String[] words2 = meetingName2.split("\\s+");

        HashSet<String> uniqueWords = new HashSet<>(Arrays.asList(words2));

        int commonWords = 0;

        for (String word : words1) {
            if (uniqueWords.contains(word)) {
                commonWords++;
            }
        }

        return commonWords;
    }

    public static String getStringInstantDateNow() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .withZone(ZoneId.of("UTC"))
                .format(Instant.now().atZone(ZoneOffset.UTC).toInstant());
    }

    public static List<PriceHistoryData> convertToPriceHistoryData(List<Float> price) {
        if (price == null) {
            return new ArrayList<>();
        }
        return price.stream().map(x -> new PriceHistoryData(x, getStringInstantDateNow())).collect(Collectors.toList());
    }

    public static boolean compareDatesAfter(Instant date1, Instant date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.isAfter(date2);
    }

    public static List<Long> convertStringToListLong(String ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return Arrays.stream(ids.trim().split(",")).map(Long::parseLong).distinct().collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static <T> void setIfPresent(T value, Consumer<T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }

    public static <T1, T2> void applyIfPresent(T1 value1, T2 value2, BiConsumer<T1, T2> consumer) {
        if (value1 != null && value2 != null) {
            consumer.accept(value1, value2);
        }
    }

    public static Json toJsonb(Object source) {
        return Json.of(new Gson().toJson(source));
    }

    public static <T> T fromJsonbToObject(Json json, TypeToken<T> typeToken) {
        Gson gson = new Gson();
        return gson.fromJson(json.asString(), typeToken.getType());
    }

    private static final Map<String, Integer> statusOrder;
    static {
        statusOrder = new HashMap<>();
        statusOrder.put(STATUS_OPEN, OPEN_STATUS_ORDER);
        statusOrder.put(STATUS_CLOSED, CLOSE_STATUS_ORDER);
        statusOrder.put(STATUS_INTERIM, INTERIM_STATUS_ORDER);
        statusOrder.put(STATUS_FINAL, FINAL_STATUS_ORDER);
        statusOrder.put(STATUS_RE_RESULTED, RE_RESULTED_STATUS_ORDER);
        statusOrder.put(STATUS_SUSPENDED, SUSPENDED_STATUS_ORDER);
        statusOrder.put(STATUS_ABANDONED, ABANDONED_STATUS_ORDER);
    }

    public static Integer getStatusOrder(String status) {
        return statusOrder.get(status);
    }

    public static List<EntrantRawData> getListEntrant(LadBrokedItRaceDto raceDto, Map<String, ArrayList<Float>> allEntrantPrices,
                                                      Map<String, LadBrokesPriceOdds> allEntrantPricePlaces, String raceId,
                                                      Map<String, Integer> positions) {
        LadbrokesMarketsRawData marketsRawData = raceDto.getMarkets().values().stream()
                .filter(m -> AppConstant.MARKETS_NAME.equals(m.getName())).findFirst()
                .orElseThrow(() -> new RuntimeException("No markets found"));

        List<EntrantRawData> result = new ArrayList<>();

        if (marketsRawData.getEntrantIds() != null) {
            marketsRawData.getEntrantIds().forEach(x -> {
                EntrantRawData data = raceDto.getEntrants().get(x);
                if (data.getFormSummary() != null && data.getId() != null) {
                    LadBrokesPriceOdds pricePlaces = allEntrantPricePlaces == null ? new LadBrokesPriceOdds() : allEntrantPricePlaces.getOrDefault(data.getId() + LADBROKE_NEDS_DATA_PRICE_KEY, new LadBrokesPriceOdds());
                    EntrantRawData entrantRawData = EntrantMapper.mapPrices(
                            data,
                            allEntrantPrices == null ? new ArrayList<>() : allEntrantPrices.getOrDefault(data.getId(), new ArrayList<>()),
                            getPricePlaces(pricePlaces),
                            positions.getOrDefault(data.getId(), 0)
                    );
                    entrantRawData.setRaceId(raceId);
                    result.add(entrantRawData);
                }
            });
        }

        return result;
    }

    public static List<Float> getPricePlaces(LadBrokesPriceOdds pricePlaces){
        DecimalFormat decimalFormat = new DecimalFormat("##.00");
        if (pricePlaces == null || pricePlaces.getOdds() == null) {
            return new ArrayList<>();
        }
        if (pricePlaces.getOdds().getNumerator() == null || pricePlaces.getOdds().getDenominator() == null) {
            return new ArrayList<>();
        }
        return Collections.singletonList(Float.parseFloat(decimalFormat.format((pricePlaces.getOdds().getNumerator() / pricePlaces.getOdds().getDenominator()) + 1F)));
    }
    public static Map<Integer, Float> getPriceFromJsonb(Json json) {
        if (json == null) {
            return new HashMap<>();
        }
        Type type = new TypeToken<Map<Integer, Float>>() {}.getType();
        return new Gson().fromJson(json.asString(), type);
    }

    public static <T, K> K applyIfNotEmpty(List<T> list, Function<List<T>, K> appliedFunction) {
        if (!CollectionUtils.isEmpty(list)) {
            try {
                return appliedFunction.apply(list);
            }catch (IndexOutOfBoundsException exception){
                return null;
            }
        }
        return null;
    }

    public static <T, K> K applyIfNotEmpty(T list, Function<T, K> appliedFunction) {
        if (list != null) {
            return appliedFunction.apply(list);
        }
        return null;
    }

    public static Float getDollarPriceFromString(String priceString, int indexPrice) {
        if (priceString == null) {
            return null;
        }
        String[] words = priceString.split(",");
        String priceWord = words[indexPrice];

        Pattern pattern = Pattern.compile(PRICE_REGEX);
        Matcher matcher = pattern.matcher(priceWord);

        if (matcher.find()) {
            String priceStr = matcher.group();
            float priceNumber = Float.parseFloat(priceStr.substring(0, priceStr.length() - 1));
            if (priceStr.endsWith("c")) {
                return priceNumber / 100;
            } else {
                return priceNumber;
            }
        }
        return null;
    }

}
