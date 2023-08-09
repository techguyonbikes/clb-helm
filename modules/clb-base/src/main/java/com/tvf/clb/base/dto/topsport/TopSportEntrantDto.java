package com.tvf.clb.base.dto.topsport;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class TopSportEntrantDto {
    private String raceUUID;
    private String entrantName;
    private Integer number;
    private Integer barrier;
    private boolean scratched;
    private Instant scratchedTime;
    private List<Float> priceWins;
    private List<Float> pricePlaces;
    private Float priceWinScratch;
    private Float pricePlacesScratch;
}
