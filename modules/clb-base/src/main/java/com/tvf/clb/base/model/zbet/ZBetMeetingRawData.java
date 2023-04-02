package com.tvf.clb.base.model.zbet;

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
public class ZBetMeetingRawData {
    @SerializedName("gbs_meeting_id")
    private String meetingId;
    private String country;
    @JsonAdapter(UpperCaseAndTrimStringDeserializer.class)
    private String name;
    private String type;
    @SerializedName("start_date")
    private String startDate;
    private String state;
    private List<ZBetRacesData> races;
}
