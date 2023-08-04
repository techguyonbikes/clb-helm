package com.tvf.clb.base.dto.kafka;

import com.tvf.clb.base.dto.EntrantResponseDto;
import com.tvf.clb.base.dto.RaceResponseDto;
import com.tvf.clb.base.dto.SiteEnum;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.utils.CommonUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@NoArgsConstructor(access= AccessLevel.PRIVATE)
public class KafkaDtoMapper {

    public static KafkaMeetingDto convertToKafkaMeetingDto(Meeting meeting) {
        return KafkaMeetingDto.builder()
                .id(meeting.getId())
                .name(meeting.getName())
                .advertisedDate(meeting.getAdvertisedDate())
                .trackCondition(meeting.getTrackCondition())
                .country(meeting.getCountry())
                .state(meeting.getState())
                .hasFixed(meeting.getHasFixed())
                .raceType(meeting.getRaceType())
                .build();
    }

    public static KafkaRaceDto convertToKafkaRaceDto(RaceResponseDto race) {
        return KafkaRaceDto.builder()
                .id(race.getId())
                .venueId(race.getVenueId())
                .status(race.getStatus())
                .advertisedStart(race.getAdvertisedStart())
                .actualStart(race.getActualStart())
                .silkUrl(race.getSilkUrl())
                .fullFormUrl(race.getFullFormUrl())
                .mapSiteUUID(convertMapSiteIdToSiteName(race.getMapSiteUUID()))
                .finalResult(convertMapSiteIdToSiteName(race.getFinalResult()))
                .interimResult(convertMapSiteIdToSiteName(race.getInterimResult()))
                .raceSiteUrl(convertMapSiteIdToSiteName(race.getRaceSiteUrl()))
                .entrants(race.getEntrants().stream().map(KafkaDtoMapper::convertToKafkaEntrantDto).collect(Collectors.toList()))
                .build();
    }

    public static KafkaRaceDto convertToKafkaRaceDto(Race race) {
        return KafkaRaceDto.builder()
                .id(race.getId())
                .venueId(race.getVenueId())
                .meetingId(race.getMeetingId())
                .name(race.getName())
                .number(race.getNumber())
                .status(race.getStatus())
                .advertisedStart(race.getAdvertisedStart() == null ? null : race.getAdvertisedStart().toString())
                .actualStart(race.getActualStart() == null ? null : race.getActualStart().toString())
                .distance(race.getDistance())
                .silkUrl(race.getSilkUrl())
                .fullFormUrl(race.getFullFormUrl())
                .build();
    }

    public static KafkaEntrantDto convertToKafkaEntrantDto(EntrantResponseDto entrant) {
        return KafkaEntrantDto.builder()
                .id(entrant.getId())
                .name(entrant.getName())
                .number(entrant.getNumber())
                .barrier(entrant.getBarrier())
                .visible(entrant.getVisible())
                .isScratched(entrant.getIsScratched())
                .scratchedTime(entrant.getScratchedTime())
                .winPriceFluctuations(convertMapSiteIdToSiteName(entrant.getPriceFluctuations()))
                .placePriceFluctuations(convertMapSiteIdToSiteName(entrant.getPricePlaces()))
                .position(entrant.getPosition())
                .riderOrDriver(entrant.getRiderOrDriver())
                .trainerName(entrant.getTrainerName())
                .last6Starts(entrant.getLast6Starts())
                .bestTime(entrant.getBestTime())
                .handicapWeight(entrant.getHandicapWeight())
                .entrantComment(entrant.getEntrantComment())
                .bestMileRate(entrant.getBestMileRate())
                .build();
    }

    public static KafkaEntrantDto convertToKafkaEntrantDto(Entrant entrant) {
        return KafkaEntrantDto.builder()
                .id(entrant.getId())
                .raceId(entrant.getRaceId())
                .name(entrant.getName())
                .number(entrant.getNumber())
                .barrier(entrant.getBarrier())
                .visible(entrant.isVisible())
                .isScratched(entrant.isScratched())
                .scratchedTime(entrant.getScratchedTime() == null ? null : entrant.getScratchedTime().toString())
                .winPriceFluctuations(convertMapSiteIdToSiteName(CommonUtils.getSitePriceFromJsonb(entrant.getPriceFluctuations())))
                .placePriceFluctuations(convertMapSiteIdToSiteName(CommonUtils.getSitePriceFromJsonb(entrant.getPricePlaces())))
                .position(entrant.getPosition())
                .riderOrDriver(entrant.getRiderOrDriver())
                .trainerName(entrant.getTrainerName())
                .last6Starts(entrant.getLast6Starts())
                .bestTime(entrant.getBestTime())
                .handicapWeight(entrant.getHandicapWeight())
                .entrantComment(entrant.getEntrantComment())
                .bestMileRate(entrant.getBestMileRate())
                .build();
    }

    /**
     * This function convert {@link Map} with key is 'siteId - Integer' to the {@link Map} with key is 'siteName - String'
     */
    public static <T> Map<String, T> convertMapSiteIdToSiteName(Map<Integer, T> map) {
        if (CollectionUtils.isEmpty(map)) {
            return Collections.emptyMap();
        }
        Map<String, T> result = new HashMap<>();
        map.forEach((siteId, value) -> result.put(SiteEnum.getSiteNameById(siteId), value));

        return result;
    }

}
