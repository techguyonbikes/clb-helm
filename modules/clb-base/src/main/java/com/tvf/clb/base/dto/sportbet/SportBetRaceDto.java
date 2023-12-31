package com.tvf.clb.base.dto.sportbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tvf.clb.base.model.sportbet.MarketRawData;
import com.tvf.clb.base.model.sportbet.ResultsRawData;
import com.tvf.clb.base.model.sportbet.SportBetDeduction;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SportBetRaceDto {
    private String name;
    List<MarketRawData>  markets;
    List<ResultsRawData> results;
    private String bettingStatus;
    private List<SportBetDeduction> deductions;
}
