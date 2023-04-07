package com.tvf.clb.base.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.r2dbc.postgresql.codec.Json;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommonUtils {

    public static Map<Integer, List<Float>> getSitePriceFromJsonb(Json json) {
        if (json == null) {
            return new HashMap<>();
        }
        Type type = new TypeToken<Map<Integer, List<Float>>>() {}.getType();
        return new Gson().fromJson(json.asString(), type);
    }

    // "NSW", "SA", "VIC", "QLD", "NT", "TAS", "WA","NZL"
    public static String checkDiffStateMeeting(String state){
        return AppConstant.VALID_CHECK_CODE_STATE_DIFF.contains(state) ? (state.equals(AppConstant.CODE_NZL) ? AppConstant.CODE_NZ : state) : null;
    }

}
