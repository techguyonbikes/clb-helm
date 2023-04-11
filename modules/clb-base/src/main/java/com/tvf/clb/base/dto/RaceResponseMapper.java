package com.tvf.clb.base.dto;

import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.entity.RaceSite;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tvf.clb.base.utils.AppConstant.ADVERTISED_START;
import static com.tvf.clb.base.utils.AppConstant.SECONDS;

@NoArgsConstructor(access= AccessLevel.PRIVATE)
public class RaceResponseMapper {

    public static RaceSite toRacesiteDto(Race race, Integer siteId, Long generalId) {
        return RaceSite.builder()
                .raceSiteId(race.getRaceId())
                .generalRaceId(generalId)
                .siteId(siteId)
                .startDate(race.getAdvertisedStart())
                .build();
    }

    public static RaceSite toRaceSiteDto(RaceDto race, Integer siteId, Long generalId) {
        return RaceSite.builder()
                .raceSiteId(race.getId())
                .generalRaceId(generalId)
                .siteId(siteId)
                .startDate(race.getAdvertisedStart())
                .build();
    }

    public static RaceResponseDto toRaceResponseDto(Race race, Map<Integer, String> mapSiteUUID, List<Entrant> entrants) {
        return RaceResponseDto.builder()
                .id(race.getId())
                .mapSiteUUID(mapSiteUUID)
                .advertisedStart(race.getAdvertisedStart().toString())
                .entrants(entrants.stream().map(EntrantMapper::toEntrantResponseDto).collect(Collectors.toList()))
                .status(race.getStatus())
                .build();
    }

    public static RaceResponseDto toRaceResponseDto(List<Entrant> entrants, String raceUUID, Long raceId, LadBrokedItRaceDto raceDto) {

        return RaceResponseDto.builder()
                .id(raceId)
                .mapSiteUUID(Collections.singletonMap(SiteEnum.LAD_BROKE.getId(), entrants.get(0).getRaceUUID()))
                .entrants(entrants.stream().map(EntrantMapper::toEntrantResponseDto).collect(Collectors.toList()))
                .advertisedStart(Instant.ofEpochSecond(raceDto.getRaces().getAsJsonObject(raceUUID).getAsJsonObject(ADVERTISED_START).get(SECONDS).getAsLong()).toString())
                .build();
    }

}
