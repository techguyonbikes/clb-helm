package com.tvf.clb.base.dto.playup;

import com.tvf.clb.base.model.playup.PlayUpRaceDtoRawData;
import com.tvf.clb.base.model.playup.PlayUpRunnerRawDataDto;
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
public class PlayUpRaceDto {
    private PlayUpRaceDtoRawData data;
    private List<PlayUpRunnerRawDataDto> included;

}
