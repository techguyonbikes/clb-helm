package com.tvf.clb.base.model.zbet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ZBetMeetingResponseRawData {

    private String timestamp;
    private List<ZBetMeetingRawData> data;
    @JsonProperty("http_status_code")
    private Long statusCode;
}
