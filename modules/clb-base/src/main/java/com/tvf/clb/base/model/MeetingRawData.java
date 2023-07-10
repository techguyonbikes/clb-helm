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

import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeetingRawData {
    private String id;
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String name;
    @JsonProperty("advertised_date")
    private String advertisedDate;
    @JsonProperty("category_id")
    private String categoryId;
    @JsonProperty("venue_id")
    private String venueId;
    @JsonProperty("race_ids")
    private List<String> raceIds;
    @JsonProperty("track_condition")
    private String trackCondition;
    private String country;
    private String state;
    @JsonProperty("has_fixed")
    private boolean hasFixed;
    @JsonProperty("region_id")
    private String regionId;
    @JsonProperty("feed_id")
    private String feedId;
    @JsonProperty("compound_ids")
    private List<String> compoundIds;
    private String racingTypeName;
}
