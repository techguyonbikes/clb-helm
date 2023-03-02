package com.tvf.clb.base.utils;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.r2dbc.postgresql.codec.Json;

import java.io.IOException;

public class PgJsonObjectSerializer extends JsonSerializer<Json> {

    @Override
    public void serialize(Json json, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        String text = json.asString();
        JsonFactory factory = new JsonFactory();
        JsonParser parser  = factory.createParser(text);
        TreeNode node = jsonGenerator.getCodec().readTree(parser);
        serializerProvider.defaultSerializeValue(node, jsonGenerator);
    }
}
