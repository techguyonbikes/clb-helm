package com.tvf.clb.base.dto;

import com.tvf.clb.base.dto.topsport.TopSportRaceDto;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.entity.RaceSite;
import com.tvf.clb.base.model.LadbrokesRaceRawData;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.CommonUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
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
                .raceSiteUrl(race.getRaceSiteUrl())
                .build();
    }

    public static RaceSite toRaceSiteDto(RaceDto race, Integer siteId, Long generalId) {
        return RaceSite.builder()
                .raceSiteId(race.getId())
                .generalRaceId(generalId)
                .siteId(siteId)
                .startDate(race.getAdvertisedStart())
                .raceSiteUrl(race.getRaceSiteUrl())
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

    public static RaceResponseDto toRaceResponseDto(List<Entrant> entrants, String raceUUID, Long raceId, RaceDto raceDto) {
        String url = raceDto.getMeetingName() + "/" + raceUUID;
        return RaceResponseDto.builder()
                .id(raceId)
                .mapSiteUUID(Collections.singletonMap(SiteEnum.LAD_BROKE.getId(), entrants.get(0).getRaceUUID()))
                .entrants(entrants.stream().map(EntrantMapper::toEntrantResponseDto).collect(Collectors.toList()))
                .advertisedStart(raceDto.getAdvertisedStart().toString())
                .finalResult(!StringUtils.hasText(raceDto.getFinalResult()) ? new HashMap<>() : Collections.singletonMap(SiteEnum.LAD_BROKE.getId(), raceDto.getFinalResult()))
                .raceSiteUrl(Collections.singletonMap(SiteEnum.LAD_BROKE.getId(), AppConstant.URL_LAD_BROKES_IT_RACE.replace(AppConstant.ID_PARAM, url)))
                .status(raceDto.getStatus())
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
    public static RaceDto toRaceDTO(TopSportRaceDto r){
        return RaceDto.builder()
                .id(r.getId())
                .raceType(r.getRaceType())
                .number(r.getRaceNumber())
                .advertisedStart(r.getStartTime())
                .name(r.getRaceName())
                .meetingName(r.getMeetingName())
                .build();
    }

    public static RaceDto toRaceDTO(LadbrokesRaceRawData raceRawData, String meetingName, String finalResult, String status) {
        return RaceDto.builder()
                .number(raceRawData.getNumber())
                .advertisedStart(raceRawData.getAdvertisedStart())
                .name(raceRawData.getName())
                .status(status)
                .finalResult(finalResult)
                .meetingName(meetingName)
                .build();
    }

}
