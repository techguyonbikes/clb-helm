package com.tvf.clb.base.model.bluebet;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
public class BlueBetEntrantRawData {
    @JsonProperty("OutcomeId")
    private Integer number;

    @JsonProperty("OutcomeName")
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String name;

    @JsonProperty("FixedPrices")
    private List<BlueBetPriceRawData> prices;

    @JsonProperty("Barrier_Box")
    private Integer barrier;

    private String trainer;

    @JsonProperty("Jockey_Driver")
    private String jockeyDriver;

    private Boolean scratched;
}
