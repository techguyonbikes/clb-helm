package com.tvf.clb.base.model.colossalbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tvf.clb.base.utils.UpperCaseAndTrimStringDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ColBetRunnerRawData {
    @JsonProperty("cid")
    private Long id;
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    @JsonProperty("rn")
    private String name;

    @JsonProperty("rs")
    private Integer number;
    @JsonProperty("rb")
    private String barrier;

    @JsonProperty("FWIN")
    private Float winPrice;

    @JsonProperty("FPLC")
    private Float placePrice;

    @JsonProperty("re")
    private Boolean isScratched;

    @JsonProperty("st")
    private String scratchedAt;

    @JsonProperty("wd")
    private Float winDeduction;

    @JsonProperty("pd")
    private Float placeDeduction;
    @JsonProperty("roFL")
    private String openPrices;

    public Float getPlaceDeduction() {
        return placeDeduction == null ? null : placeDeduction / 100;
    }

    public Float getWinDeduction() {
        return winDeduction == null ? null : winDeduction / 100;
    }
}
