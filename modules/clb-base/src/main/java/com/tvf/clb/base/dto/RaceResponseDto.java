package com.tvf.clb.base.dto;

import lombok.*;
import org.springframework.data.redis.core.RedisHash;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("race")
public class RaceResponseDto {
    private Long id;
    private String raceName;
    private Integer raceNumber;
    private String raceType;
    private Integer distance;
    private String status;
    private String advertisedStart;
    private String actualStart;
    private String silkUrl;
    private String fullFormUrl;
    private String venueId;
    private Map<Integer, String> mapSiteUUID;
    private Map<Integer, String> finalResult;
    private Map<Integer, String> interimResult;
    private Map<Integer, String> raceSiteUrl;
    private List<EntrantResponseDto> entrants;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RaceResponseDto that = (RaceResponseDto) o;
        return Objects.equals(id, that.id) && Objects.equals(raceName, that.raceName) && Objects.equals(raceNumber, that.raceNumber) && Objects.equals(raceType, that.raceType) && Objects.equals(distance, that.distance) && Objects.equals(status, that.status) && Objects.equals(advertisedStart, that.advertisedStart) && Objects.equals(actualStart, that.actualStart) && Objects.equals(silkUrl, that.silkUrl) && Objects.equals(fullFormUrl, that.fullFormUrl) && Objects.equals(venueId, that.venueId) && Objects.equals(mapSiteUUID, that.mapSiteUUID) && Objects.equals(finalResult, that.finalResult) && Objects.equals(interimResult, that.interimResult) && Objects.equals(raceSiteUrl, that.raceSiteUrl) && Objects.equals(entrants, that.entrants);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, raceName, raceNumber, raceType, distance, status, advertisedStart, actualStart, silkUrl, fullFormUrl, venueId, mapSiteUUID, finalResult, interimResult, raceSiteUrl, entrants);
    }
}
