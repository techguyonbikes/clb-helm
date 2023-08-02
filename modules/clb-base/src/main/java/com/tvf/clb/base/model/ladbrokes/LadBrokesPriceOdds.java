package com.tvf.clb.base.model.ladbrokes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LadBrokesPriceOdds {

    private LadBrokesPricePacesRawData odds;
}
