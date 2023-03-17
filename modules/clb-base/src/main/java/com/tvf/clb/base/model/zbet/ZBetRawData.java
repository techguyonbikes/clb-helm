package com.tvf.clb.base.model.zbet;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ZBetRawData {
    private List<ZBetMeetingRawData> data;
    @SerializedName("http_status_code")
    private int statusCode;
    private String timestamp;
}
