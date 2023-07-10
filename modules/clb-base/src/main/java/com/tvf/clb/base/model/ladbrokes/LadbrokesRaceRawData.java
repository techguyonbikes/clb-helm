package com.tvf.clb.base.model.ladbrokes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tvf.clb.base.utils.UpperCaseAndTrimStringDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LadbrokesRaceRawData {

    private String id;
    @JsonProperty("meeting_id")
    private String meetingId;
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String name;
    private Integer number;
    @JsonProperty("advertised_start")
    private Instant advertisedStart;
    @JsonProperty("actual_start")
    private Instant actualStart;
    @JsonProperty("market_ids")
    private List<String> marketIds;
    @JsonProperty("additional_info")
    private JsonNode additionalInfo;
    @JsonProperty("silk_url")
    private String silkUrl;
    @JsonProperty("full_form_url")
    private String fullFormUrl;
    private List<LadbrokesRaceDividend> dividends;
}
