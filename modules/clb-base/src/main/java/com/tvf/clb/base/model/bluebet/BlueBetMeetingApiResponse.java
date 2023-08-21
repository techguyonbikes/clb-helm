package com.tvf.clb.base.model.bluebet;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
public class BlueBetMeetingApiResponse {
    @JsonProperty("Thoroughbred")
    private List<List<BlueBetRaceRawData>> horseRacing;

    @JsonProperty("Greyhounds")
    private List<List<BlueBetRaceRawData>> greyhoundRacing;

    @JsonProperty("Trots")
    private List<List<BlueBetRaceRawData>> harnessRacing;
}
