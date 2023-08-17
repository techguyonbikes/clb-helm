package com.tvf.clb.base.dto.playup;

import com.tvf.clb.base.model.playup.PlayUpMeetingDtoRawData;
import com.tvf.clb.base.model.playup.PlayUpRaceDtoRawData;
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
public class PlayUpMeetingDto {
    private Object meta;
    private List<PlayUpMeetingDtoRawData> data;
    private List<PlayUpRaceDtoRawData> included;
}
