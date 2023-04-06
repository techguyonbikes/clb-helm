package com.tvf.clb.base.model.sportbet;

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
public class SportBetPriceRawData {
    private String priceCode;
    private Float winPrice;
    private Float placePrice;
}
