package com.tvf.clb.base.model.betright;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tvf.clb.base.utils.UpperCaseAndTrimStringDeserializer;
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
public class BetRightEntrantRawData {

    private Integer outcomeId;
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String outcomeName;
    private List<BetRightPricesRawData> fixedPrices;
    private String trainer;
    private String jockeyDriver;
    private Integer barrierBox;
    private Float weight;
    private Boolean scratched;

}
