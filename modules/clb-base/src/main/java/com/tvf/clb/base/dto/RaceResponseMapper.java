package com.tvf.clb.base.dto;

import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.entity.RaceSite;
import org.springframework.stereotype.Component;

@Component
public class RaceResponseMapper {

    private static final String SIDE_NAME_PREFIX = "R";

    public static RaceSite toRacesiteDto(Race race, Integer siteId, Long generalId) {
        return RaceSite.builder()
                .raceSiteId(race.getRaceId())
                .generalRaceId(generalId)
                .siteId(siteId)
                .startDate(race.getAdvertisedStart())
                .build();
    }

}
