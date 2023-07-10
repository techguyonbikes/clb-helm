package com.tvf.clb.base.model.tab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class TabRacesData {
    private Integer raceNumber;
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String raceName;
    private String raceClassConditions;
    private String raceStartTime;
    private String raceStatus;
    private Integer raceDistance;
    private boolean hasParimutuel;
    private boolean hasFixedOdds;
    private String broadcastChannel;
    private skyRacing skyRacing;
    private boolean willHaveFixedOdds;
    private boolean allIn;
    private boolean allowBundle;
    private String cashOutEligibility;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    class skyRacing {
        private String audio;
        private String video;
    }
}