package com.tvf.clb.base.model.bluebet;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
public class BlueBetDeductionRawData {
    @JsonProperty("OutcomeId")
    private Integer entrantNumber;

    @JsonProperty("DeductionWin")
    private Float win;

    @JsonProperty("DeductionPlace1")
    private Float place1;

    @JsonProperty("DeductionPlace2")
    private Float place2;

    @JsonProperty("DeductionPlace3")
    private Float place3;
}
