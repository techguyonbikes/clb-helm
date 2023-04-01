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

    private List<List<Integer>> results;
    private List<RunnerRawData> runners;

    public List<Integer> getResults() {
        List<Integer> flattenedList = new ArrayList<>();
        for (List<Integer> innerList : results) {
            flattenedList.addAll(innerList);
        }
        return flattenedList;
    }

    public List<RunnerRawData> getRunners() {
        return runners;
    }
}
