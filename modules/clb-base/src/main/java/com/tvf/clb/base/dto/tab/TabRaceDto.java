package com.tvf.clb.base.dto.tab;

import com.tvf.clb.base.model.tab.RunnerTabRawData;
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
public class TabRaceDto {

    private String raceName;
    List<RunnerTabRawData>  runners;

    List<Integer> results;






}
