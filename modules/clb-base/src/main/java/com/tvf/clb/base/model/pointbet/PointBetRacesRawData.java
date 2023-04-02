package com.tvf.clb.base.model.pointbet;

import com.google.gson.annotations.JsonAdapter;
import com.tvf.clb.base.utils.UpperCaseAndTrimStringDeserializer;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PointBetRacesRawData {
    private String eventId;
    private Integer raceNumber;
    @JsonAdapter(UpperCaseAndTrimStringDeserializer.class)
    private String name;
    private String advertisedStartDateTime;
    private String bettingCloseTime;
    private Integer tradingStatus;
    private Integer resultStatus;
    private String placing;
    private String allowedBetTypes;
    private String version;

}
