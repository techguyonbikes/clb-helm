package com.tvf.clb.base.dto;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.tvf.clb.base.model.EntrantRawData;
import com.tvf.clb.base.model.MarketsRawData;
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
public class LadBrokedItRaceDto {
    private JsonObject races;
    private Map<String, MarketsRawData> markets;
    private Object meetings;
    private Object prices;
    private Object venues;
    private Object substitutions;
    private JsonObject results;
    private HashMap<String, EntrantRawData> entrants;
    @SerializedName("price_fluctuations")
    private HashMap<String, ArrayList<Float>> priceFluctuations;
}
