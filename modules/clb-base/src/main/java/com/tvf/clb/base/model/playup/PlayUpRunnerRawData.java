package com.tvf.clb.base.model.playup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tvf.clb.base.utils.UpperCaseAndTrimStringDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayUpRunnerRawData {
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String id;
    private String name;
    private Integer number;
    private Integer barrier;
    private Float winPrice;
    private Float placePrice;
    private PlayUpDeductionsRawData deductions;
}
