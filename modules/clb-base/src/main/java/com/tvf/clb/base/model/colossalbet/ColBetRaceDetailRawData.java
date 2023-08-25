package com.tvf.clb.base.model.colossalbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tvf.clb.base.utils.UpperCaseAndTrimStringDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ColBetRaceDetailRawData {
    @JsonProperty("eid")
    private Long id;

    @JsonProperty("rnm")
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String name;
    @JsonProperty("rn")
    private String number;

    @JsonProperty("roa")
    private String startTime;
    @JsonProperty("rs")
    private String status;
    private String result;
    @JsonProperty("rt")
    private String type;
    @JsonProperty("rd")
    private String distance;
    @JsonProperty("rst")
    private String state;

    @JsonProperty("rm")
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String meetingName;

    @JsonProperty("cc")
    private String additionalComments;

    @JsonProperty("rnnr")
    private List<ColBetRunnerRawData> runners;
}
