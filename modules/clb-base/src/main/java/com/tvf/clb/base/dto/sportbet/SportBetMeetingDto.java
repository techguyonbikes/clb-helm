package com.tvf.clb.base.dto.sportbet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.tvf.clb.base.model.sportbet.SportBetMeetingRawData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SportBetMeetingDto {
    private String displayName;
    private Integer displayOrder;
    private String raceType;
    private List<SportBetMeetingRawData> meetings;
}
