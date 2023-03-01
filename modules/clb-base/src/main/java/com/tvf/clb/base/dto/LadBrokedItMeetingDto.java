package com.tvf.clb.base.dto;

import com.google.gson.annotations.SerializedName;
import com.tvf.clb.base.model.MeetingRawData;
import com.tvf.clb.base.model.RaceRawData;
import com.tvf.clb.base.model.VenueRawData;
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
    private HashMap<String, MeetingRawData> meetings;
    private HashMap<String, RaceRawData> races;
    private HashMap<String, VenueRawData>  venues;
}
