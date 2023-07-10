package com.tvf.clb.base.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FormSummaryRawData {
    private String last20Starts;
    private String riderOrDriver;
    private String trainerName;
    private String bestTime;
    private Float handicapWeight;
    private String entrantComment;
    private Object speedmap;
    private String bestMileRate;
}
