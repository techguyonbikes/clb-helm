package com.tvf.clb.base.model.zbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZBetResultsRawData {
    private Integer position;
    private int number;
    private Long product_id;
    private String name;
    private Long selection_id;
    private String place_dividend;
}
