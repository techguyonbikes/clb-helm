package com.tvf.clb.base.model.tab;

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
public class TabPriceRawData {

    private Float returnWin;
    private String returnWinTime;
    private String bettingStatus;
    private List<TabPriceFlucsRawData> flucs;
    private String scratchedTime;
}
