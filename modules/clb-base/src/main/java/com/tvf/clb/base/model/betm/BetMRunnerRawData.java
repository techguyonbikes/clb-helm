package com.tvf.clb.base.model.betm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tvf.clb.base.utils.UpperCaseAndTrimStringDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetMRunnerRawData {
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String name;

    @JsonProperty("saddle_number")
    private Integer number;

    private String barrier;

    @JsonProperty("fwin")
    private Float winPrice;

    @JsonProperty("fplc")
    private Float placePrice;

    @JsonProperty("runner_eliminated")
    private Boolean isScratched;

    @JsonProperty("scratched_at")
    private Instant scratchedAt;

    @JsonProperty("win_deduction")
    private Float winDeduction;

    @JsonProperty("place_deduction")
    private Float placeDeduction;

    private Integer weight;

    public Float getPlaceDeduction() {
        return placeDeduction == null ? null : placeDeduction / 100;
    }

    public Float getWinDeduction() {
        return winDeduction == null ? null : winDeduction / 100;
    }
}
