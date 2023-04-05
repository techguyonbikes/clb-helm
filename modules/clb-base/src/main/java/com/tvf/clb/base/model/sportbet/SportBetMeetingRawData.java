package com.tvf.clb.base.model.sportbet;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
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
public class SportBetMeetingRawData {

    private Long id;
    @JsonAdapter(UpperCaseAndTrimStringDeserializer.class)
    private String name;
    private int classId;
    private boolean isInternational;
    private String className;
    private String regionName;
    private Boolean streamingAvailable;

    private String availableStreamingType;
    private Boolean mbsAvailable;

    private List<SportBetRacesData> events;
}
