package com.tvf.clb.base.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Table("meeting")
public class Meeting {
    @Id
    private String id;
    private String name;
    @SerializedName("advertised_date")
    private String advertisedDate;
    @SerializedName("category_id")
    private String categoryId;
    @SerializedName("venue_id")
    private String venueId;
    @Transient
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
    @Transient
    @SerializedName("compound_ids")
    private List<String> compoundIds;
}
