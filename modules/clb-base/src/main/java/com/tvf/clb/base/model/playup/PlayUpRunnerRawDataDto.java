package com.tvf.clb.base.model.playup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayUpRunnerRawDataDto {
    private String id;
    private String type;
    private JsonNode attributes;
    private PlayUpRelationshipsRawData relationships;
}
