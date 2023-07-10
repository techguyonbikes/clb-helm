package com.tvf.clb.base.model.pointbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tvf.clb.base.utils.UpperCaseAndTrimStringDeserializer;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class PointBetEntrantRawData {
    @JsonProperty("outcomeId")
    private String id;
    @JsonProperty("outcomeName")
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String name;
    @JsonProperty("fixedPrices")
    private List<PointBetEntrantPrice> prices;
    private Integer barrierBox;
    private boolean scratched;
    private String rating;
    private PointBetEntrantDeduction deduction;
    private Integer position;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
class PointBetEntrantDeduction {
    private String scratchedTime;
    private Float win;
    private Float place1;
    private Float place2;
    private Float place3;
}
