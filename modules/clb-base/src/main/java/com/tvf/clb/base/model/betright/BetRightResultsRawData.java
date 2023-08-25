package com.tvf.clb.base.model.betright;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetRightResultsRawData {

    private List<BetRightWinnersRawData> winners;
    private List<BetRightDeductionsRawData> deductions;
}
