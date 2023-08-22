package com.tvf.clb.base.model.betright;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetRightPricesRawData {

    private String marketTypeCode;
    private Float price;
    private Float points;
    private Float openPrice;
    private Float currentFluc;
    private Float currentFluc1;
    private Float currentFluc2;
}
