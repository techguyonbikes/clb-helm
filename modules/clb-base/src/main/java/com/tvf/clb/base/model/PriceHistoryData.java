package com.tvf.clb.base.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.Objects;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PriceHistoryData {

    private Float price;
    private String dateHistory;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PriceHistoryData)) return false;
        PriceHistoryData that = (PriceHistoryData) o;
        return price.equals(that.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(price);
    }
}
