package com.tvf.clb.base.model.zbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.gson.JsonElement;
import com.tvf.clb.base.utils.UpperCaseAndTrimStringDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZBetEntrantData {

    private Long id;
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String name;
    private Integer number;
    private Integer barrier;
    @JsonProperty("scratching_time")
    private String scratchingTime;
    @JsonProperty("selection_status")
    private String selectionsStatus;
    private JsonNode prices;
}
