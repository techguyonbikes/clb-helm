package com.tvf.clb.base.dto;

import com.google.gson.annotations.SerializedName;
import com.tvf.clb.base.model.Entrant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.HashMap;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LadBrokedItRaceDto {
    private Object races;
    private Object markets;
    private Object meetings;
    private Object prices;
    private Object venues;
    private Object substitutions;
    private Object results;
    private HashMap<String, Entrant> entrants;
    @SerializedName("price_fluctuations")
    private HashMap<String, ArrayList<Float>> priceFluctuations;
}
