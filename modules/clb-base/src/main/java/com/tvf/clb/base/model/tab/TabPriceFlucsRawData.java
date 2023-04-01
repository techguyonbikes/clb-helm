package com.tvf.clb.base.model.tab;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TabPriceFlucsRawData {

    private Float returnWin;
    private String returnWinTime;
}
