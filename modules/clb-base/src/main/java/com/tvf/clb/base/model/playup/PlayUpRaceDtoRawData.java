package com.tvf.clb.base.model.playup;

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
public class PlayUpRaceDtoRawData {
    private Long id;
    private String type;
    private PlayUpRaceRawData attributes;
    private Object links;
    private Object relationships;
}
