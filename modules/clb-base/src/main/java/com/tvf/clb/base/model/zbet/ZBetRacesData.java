package com.tvf.clb.base.model.zbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
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
public class ZBetRacesData {
    private Long id;
    private Integer number;
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String name;
    private String meetingName;
    @JsonProperty("start_date")
    private String startDate;
    private String type;
    private String status;
    @JsonProperty("race_site_link")
    private String raceSiteLink;


}
