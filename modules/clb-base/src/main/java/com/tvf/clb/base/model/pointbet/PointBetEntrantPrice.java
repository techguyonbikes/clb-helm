package com.tvf.clb.base.model.pointbet;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PointBetEntrantPrice {
    private String marketTypeCode;
    private Float price;
    private Float points;
    private String fixedMarketId;
    private String openPrice;
    private Float currentFluc;
    private Float currentFluc1;
    private Float currentFluc2;
    private Boolean isCashoutAllowed;
    private List<PointBetPriceFluctuation> flucs;
}
