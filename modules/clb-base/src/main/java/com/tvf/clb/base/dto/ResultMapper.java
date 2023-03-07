package com.tvf.clb.base.dto;

import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Results;
import com.tvf.clb.base.model.EntrantRawData;
import com.tvf.clb.base.model.ResultsRawData;
import io.r2dbc.postgresql.codec.Json;

import java.util.ArrayList;
import java.util.List;

public class ResultMapper {

    public static Results toResultsEntity(ResultsRawData resultsRawData) {
        return Results.builder()
                .entrantId(resultsRawData.getEntranId())
                .raceId(resultsRawData.getRaceId())
                .marketId(resultsRawData.getMarketId())
                .position(resultsRawData.getPosition())
                .resultStatusId(resultsRawData.getResultStatusId())
                .build();
    }
    public static ResultsRawData mapRaceID(ResultsRawData resultsRawData, String id) {
        return ResultsRawData.builder()
                .entranId(resultsRawData.getEntranId())
                .raceId(id)
                .marketId(resultsRawData.getMarketId())
                .position(resultsRawData.getPosition())
                .resultStatusId(resultsRawData.getResultStatusId())
                .build();
    }
}
