package com.tvf.clb.base.model;

import com.google.gson.annotations.SerializedName;
import com.tvf.clb.base.entity.AdditionalInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class RaceRawData {
    private String id;
    @SerializedName("meeting_id")
    private String meetingId;
    private String name;
    private Integer number;
    @SerializedName("advertised_start")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private Instant advertisedStart;
    @SerializedName("actual_start")
    private Instant actualStart;
    @SerializedName("market_ids")
    private List<String> marketIds;
    @SerializedName("main_market_status_id")
    private String mainMarketStatusId;
    @SerializedName("results_display")
    private String resultsDisplay;
    @SerializedName("additional_info")
    private AdditionalInfoDataRaw additionalInfo;
}


