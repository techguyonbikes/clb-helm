package com.tvf.clb.base.model.tab;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TabRunnerRawData {

    private Integer raceDistance;
    private List<List<Integer>> results;
    private List<RunnerTabRawData> runners;

    public List<Integer> getResults() {
        List<Integer> flattenedList = new ArrayList<>();
        if (results != null) {
            for (List<Integer> innerList : results) {
                flattenedList.addAll(innerList);
            }
            return flattenedList;
        }
        return null;
    }

    public List<RunnerTabRawData> getRunners() {
        return runners;
    }

    public Integer getRaceDistance() {
        return raceDistance;
    }
}
