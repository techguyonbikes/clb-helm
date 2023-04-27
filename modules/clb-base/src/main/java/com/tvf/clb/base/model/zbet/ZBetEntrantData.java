package com.tvf.clb.base.model.zbet;

import com.google.gson.JsonElement;
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
public class ZBetEntrantData {

    private Long id;
    @JsonAdapter(UpperCaseAndTrimStringDeserializer.class)
    private String name;
    private Integer number;
    private Integer barrier;
    @SerializedName("scratching_time")
    private String scratchingTime;
    @SerializedName("selection_status")
    private String selectionsStatus;
    private JsonElement prices;
}
