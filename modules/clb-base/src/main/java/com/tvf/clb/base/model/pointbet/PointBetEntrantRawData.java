package com.tvf.clb.base.model.pointbet;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.tvf.clb.base.utils.UpperCaseAndTrimStringDeserializer;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PointBetEntrantRawData {
    @SerializedName("outcomeId")
    private String id;
    @SerializedName("outcomeName")
    @JsonAdapter(UpperCaseAndTrimStringDeserializer.class)
    private String name;
    @SerializedName("fixedPrices")
    private List<PointBetEntrantPrice> prices;
    private Integer barrierBox;
    private boolean scratched;
    private String rating;
    private PointBetEntrantDeduction deduction;
    private Integer position;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class PointBetEntrantDeduction {
    private String scratchedTime;
    private Float win;
    private Float place1;
    private Float place2;
    private Float place3;
}
