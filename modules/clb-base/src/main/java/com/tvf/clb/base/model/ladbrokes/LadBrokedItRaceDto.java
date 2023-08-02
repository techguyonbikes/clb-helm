package com.tvf.clb.base.model.ladbrokes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.tvf.clb.base.model.EntrantRawData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LadBrokedItRaceDto {
    private Map<String, LadbrokesRaceRawData> races;
    private Map<String, LadbrokesMarketsRawData> markets;
    private JsonNode meetings;
    private Object venues;
    private Object substitutions;
    private Map<String, LadbrokesRaceResult> results;
    private HashMap<String, EntrantRawData> entrants;
    @JsonProperty("price_fluctuations")
    private HashMap<String, ArrayList<Float>> priceFluctuations;
    @JsonProperty("prices")
    private HashMap<String, LadBrokesPriceOdds> pricePlaces;
}
