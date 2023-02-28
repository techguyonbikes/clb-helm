package com.tvf.clb.base.utils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ApiUtils {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36";
    public static Response get(String url, Map<String, String> headers) throws IOException {
        Headers requestHeaders = getHeaders(headers);
        Request request = new Request.Builder().url(url).headers(requestHeaders).get().build();
        return send(request);
    }

    public static Response get(String url) throws IOException {
        return get(url, null);
    }

    private static Headers getHeaders(Map<String, String> headers) {
        Map<String, String> defaultHeaders = new HashMap<>();
        defaultHeaders.put("Content-type", "application/json");
        defaultHeaders.put("user-agent", USER_AGENT);
        if (headers != null) {
            defaultHeaders.putAll(headers);
        }

        return Headers.of(defaultHeaders);
    }

    private static Response send(Request request) throws IOException {
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        httpClientBuilder.addInterceptor(forwardHeaders());

        OkHttpClient client = httpClientBuilder.build();
        Call call = client.newCall(request);
        return call.execute();
    }

    private static Interceptor forwardHeaders() {
        return chain -> {
            Request original = chain.request();

            Headers mergedHeaders;
            if (HeadersContext.headers.get() != null) {
                Headers originalHeaders = original.headers();
                Headers additionalHeaders = Headers.of(HeadersContext.headers.get());
                mergedHeaders = originalHeaders.newBuilder().addAll(additionalHeaders).build();
            } else {
                mergedHeaders = original.headers();
            }

            Request request = original.newBuilder()
                    .headers(mergedHeaders)
                    .method(original.method(), original.body())
                    .build();
            return chain.proceed(request);
        };
    }
}
