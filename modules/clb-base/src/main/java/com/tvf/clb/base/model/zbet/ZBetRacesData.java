package com.tvf.clb.base.model.zbet;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ZBetRacesData {
    private Long id;
    private Integer number;
    private String name;
    @SerializedName("start_date")
    private String startDate;
    private String type;
    private String status;
}
