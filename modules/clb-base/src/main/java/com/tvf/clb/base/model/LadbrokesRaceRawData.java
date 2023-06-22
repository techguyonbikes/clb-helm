package com.tvf.clb.base.model;

import com.google.gson.JsonObject;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.tvf.clb.base.utils.UpperCaseAndTrimStringDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LadbrokesRaceRawData {

    private String id;
    @SerializedName("meeting_id")
    private String meetingId;
    @JsonAdapter(UpperCaseAndTrimStringDeserializer.class)
    private String name;
    private Integer number;
    @SerializedName("advertised_start")
    private Instant advertisedStart;
    @SerializedName("actual_start")
    private Instant actualStart;
    @SerializedName("market_ids")
    private List<String> marketIds;
    @SerializedName("additional_info")
    private JsonObject additionalInfo;
    @SerializedName("silk_url")
    private String silkUrl;
    @SerializedName("full_form_url")
    private String fullFormUrl;
    private List<LadbrokesRaceDividend> dividends;
}
