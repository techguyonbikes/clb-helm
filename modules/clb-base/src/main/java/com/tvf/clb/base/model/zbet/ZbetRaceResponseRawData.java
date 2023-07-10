package com.tvf.clb.base.model.zbet;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ZbetRaceResponseRawData {

    private String timestamp;
    private ZBetRaceRawData data;
    @JsonProperty("http_status_code")
    private Long statusCode;
}
