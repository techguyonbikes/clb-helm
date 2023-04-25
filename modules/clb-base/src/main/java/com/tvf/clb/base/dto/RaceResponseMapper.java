package com.tvf.clb.base.dto;

import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.entity.RaceSite;
import com.tvf.clb.base.utils.CommonUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collections;
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

    public static RaceResponseDto toRaceResponseDto(Race race, Map<Integer, String> mapSiteUUID, List<Entrant> entrants) {
        return RaceResponseDto.builder()
                .id(race.getId())
                .mapSiteUUID(mapSiteUUID)
                .advertisedStart(race.getAdvertisedStart().toString())
                .finalResult(CommonUtils.getMapRaceFinalResultFromJsonb(race.getResultsDisplay()))
                .entrants(entrants.stream().map(EntrantMapper::toEntrantResponseDto).collect(Collectors.toList()))
                .status(race.getStatus())
                .build();
    }

    public static RaceResponseDto toRaceResponseDto(List<Entrant> entrants, String raceUUID, Long raceId, LadBrokedItRaceDto raceDto) {

        return RaceResponseDto.builder()
                .id(raceId)
                .mapSiteUUID(Collections.singletonMap(SiteEnum.LAD_BROKE.getId(), entrants.get(0).getRaceUUID()))
                .entrants(entrants.stream().map(EntrantMapper::toEntrantResponseDto).collect(Collectors.toList()))
                .advertisedStart(raceDto.getRaces().get(raceUUID).getAdvertisedStart().toString())
                .build();
    }

    public static RaceDto toRaceDTO(Race r){
        return RaceDto.builder()
                .raceType(r.getRaceType())
                .number(r.getNumber())
                .advertisedStart(r.getAdvertisedStart())
                .name(r.getName())
                .build();
    }

}
