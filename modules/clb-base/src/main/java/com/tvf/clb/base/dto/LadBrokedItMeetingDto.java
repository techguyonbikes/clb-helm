package com.tvf.clb.base.dto;

import com.google.gson.annotations.SerializedName;
import com.tvf.clb.base.model.Meeting;
import com.tvf.clb.base.model.Race;
import com.tvf.clb.base.model.Venue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class LadBrokedItMeetingDto {
    private Object compounds;
    @SerializedName("domestic_countries")
    private String domesticCountries;
    private HashMap<String, Meeting> meetings;
    private HashMap<String, Race> races;
    private HashMap<String, Venue>  venues;
}
