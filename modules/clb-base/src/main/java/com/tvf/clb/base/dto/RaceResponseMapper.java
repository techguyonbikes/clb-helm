package com.tvf.clb.base.dto;

import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.entity.RaceSite;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public static RaceResponseDto toraceResponseDto(Race race, Map<Integer, String> mapSiteUUID, List<Entrant> entrants) {
        return RaceResponseDto.builder()
                .id(race.getId())
                .mapSiteUUID(mapSiteUUID)
                .entrants(entrants.stream().map(EntrantMapper::toEntrantResponseDto).collect(Collectors.toList()))
                .status(race.getStatus())
                .build();
    }

}
