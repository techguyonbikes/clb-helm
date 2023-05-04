package com.tvf.clb.base.model;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
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
public class MeetingRawData {
    private String id;
    @JsonAdapter(UpperCaseAndTrimStringDeserializer.class)
    private String name;
    @SerializedName("advertised_date")
    private String advertisedDate;
    @SerializedName("category_id")
    private String categoryId;
    @SerializedName("venue_id")
    private String venueId;
    @SerializedName("race_ids")
    private List<String> raceIds;
    @SerializedName("track_condition")
    private String trackCondition;
    private String country;
    private String state;
    @SerializedName("has_fixed")
    private boolean hasFixed;
    @SerializedName("region_id")
    private String regionId;
    @SerializedName("feed_id")
    private String feedId;
    @SerializedName("compound_ids")
    private List<String> compoundIds;
    private String racingTypeName;
}
