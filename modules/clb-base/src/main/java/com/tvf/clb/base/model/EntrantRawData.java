package com.tvf.clb.base.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tvf.clb.base.utils.UpperCaseAndTrimStringDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntrantRawData {
    private String id;
    private String raceId;
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String name;
    private int barrier;
    private int number;
    @JsonProperty("market_id")
    private String marketId;
    private boolean visible;

    @JsonProperty("pricefluctuations")
    private List<Float> priceFluctuations;
    @JsonProperty("form_summary")
    public FormSummaryRawData formSummary;
    @JsonProperty("is_scratched")
    private String isScratched;
    @JsonProperty("scratch_time")
    private Instant scratchedTime;
    @JsonProperty("position")
    private Integer position;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntrantRawData)) return false;

        EntrantRawData that = (EntrantRawData) o;

        if (barrier != that.barrier) return false;
        if (number != that.number) return false;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + barrier;
        result = 31 * result + number;
        return result;
    }
}

