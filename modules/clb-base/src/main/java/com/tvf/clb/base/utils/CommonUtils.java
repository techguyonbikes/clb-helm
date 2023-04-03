package com.tvf.clb.base.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.r2dbc.postgresql.codec.Json;

import java.lang.reflect.Type;
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

}
