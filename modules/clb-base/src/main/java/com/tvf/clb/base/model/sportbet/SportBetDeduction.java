package com.tvf.clb.base.model.sportbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SportBetDeduction {
    private Integer runnerNumber;
    @JsonProperty("value")
    private Float winDeduction;
    private Float placeDeduction;
}
