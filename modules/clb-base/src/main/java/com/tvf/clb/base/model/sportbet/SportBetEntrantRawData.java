package com.tvf.clb.base.model.sportbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tvf.clb.base.utils.UpperCaseAndTrimStringDeserializer;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class SportBetEntrantRawData {
    private Long id;
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String name;
    private Integer runnerNumber;
    private Integer drawNumber;
    private List<SportBetPriceRawData> prices;
    private String result;
    private Float placePrice;
    private StatisticsRawData statistics;

}
