package com.tvf.clb.base.dto;

import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Results;
import com.tvf.clb.base.model.ResultsRawData;
import io.r2dbc.postgresql.codec.Json;

import java.util.ArrayList;

public class ResultMapper {

    public static Results toResultsEntity(ResultsRawData resultsRawData) {
        return Results.builder()
                .entrantId(resultsRawData.getEntranId())
                .marketId(resultsRawData.getMarketId())
                .position(resultsRawData.getPosition())
                .resultStatusId(resultsRawData.getResultStatusId())
                .build();
    }
}
