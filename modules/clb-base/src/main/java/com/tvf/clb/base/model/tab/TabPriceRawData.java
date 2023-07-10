package com.tvf.clb.base.model.tab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TabPriceRawData {

    private Float returnWin;
    private String returnWinTime;
    private String bettingStatus;
    private List<TabPriceFlucsRawData> flucs;
    private String scratchedTime;
}
