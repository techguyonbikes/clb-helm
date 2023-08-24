package com.tvf.clb.base.model.betright;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tvf.clb.base.utils.UpperCaseAndTrimStringDeserializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BetRightMeetingRaceTypeRawData {

    private List<BetRightMeetingRacesRawData> races;
    private String categoryId;
    private String masterEventId;
    private Boolean hasRacingSpecials;
    private String racingSpecialsMasterEventId;
    private String masterCategoryName;
    private String venueId;
    @JsonDeserialize(using = UpperCaseAndTrimStringDeserializer.class)
    private String venue;
    private String countryCode;
}
