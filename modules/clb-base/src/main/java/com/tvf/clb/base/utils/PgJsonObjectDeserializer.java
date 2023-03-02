package com.tvf.clb.base.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import io.r2dbc.postgresql.codec.Json;

import java.io.IOException;

public class PgJsonObjectDeserializer extends JsonDeserializer<Json> {

    @Override
    public Json deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode value = ctxt.readTree(p);
        return Json.of(value.toString());
    }
}
