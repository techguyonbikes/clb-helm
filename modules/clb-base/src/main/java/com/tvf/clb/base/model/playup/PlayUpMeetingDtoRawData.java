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
public class PlayUpMeetingDtoRawData {
    private Long id;
    private String type;
    private PlayUpMeetingRawData attributes;
    private Object relationships;
    private Object links;
}
