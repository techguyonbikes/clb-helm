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
public class ZBetRaceRawData {
    private Long id;
    private Integer number;
    private String name;
    private Integer distance;
    private String status;
    private List<Deductions> deductions;
    private List<ZBetEntrantData> selections;

    @SerializedName("displayed_results")
    private List<ZBetResultsRawData> displayedResults;

    @SerializedName("result_string")
    private String finalResult;
}


