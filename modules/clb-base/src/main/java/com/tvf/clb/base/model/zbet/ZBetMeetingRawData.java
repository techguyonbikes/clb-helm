package com.tvf.clb.base.model.zbet;

import com.google.gson.annotations.SerializedName;
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
    private String name;
    private String type;
    @SerializedName("start_date")
    private String startDate;
    private String state;
    private List<ZBetRacesData> races;
}
