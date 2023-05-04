package com.tvf.clb.base.model.zbet;

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
public class ZBetRacesData {
    private Long id;
    private Integer number;
    @JsonAdapter(UpperCaseAndTrimStringDeserializer.class)
    private String name;
    private String meetingName;
    @SerializedName("start_date")
    private String startDate;
    private String type;
    private String status;
    @SerializedName("race_site_link")
    private String raceSiteLink;


}
