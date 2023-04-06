package com.tvf.clb.base.model.sportbet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SportBetEntrantRawData {
    private Long id;
    private String name;
    private Integer runnerNumber;
    private Integer drawNumber;
    private List<SportBetPriceRawData> prices;
    private String result;
    private Float placePrice;
    private StatisticsRawData statistics;

}
