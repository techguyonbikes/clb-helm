package com.tvf.clb.base.model.sportbet;

import com.google.gson.annotations.JsonAdapter;
import com.tvf.clb.base.utils.UpperCaseAndTrimStringDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SportBetRacesData {
    private Long id;
    private Integer raceNumber;
    @JsonAdapter(UpperCaseAndTrimStringDeserializer.class)
    private String name;
    private String result;
    private Long startTime;
    private String statusCode;
    private Integer distance;
    private boolean streamingAvailable;
    private boolean hasFixedOdds;
    private String availableStreamingType;
    private boolean mbsAvailable;
    private String regionGroup;
    private String type;
    private String category;
    private String displayName;
}