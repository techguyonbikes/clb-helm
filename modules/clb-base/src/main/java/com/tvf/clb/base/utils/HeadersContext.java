package com.tvf.clb.base.utils;

import com.tvf.clb.base.exception.NotFoundException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@NoArgsConstructor(access= AccessLevel.PRIVATE)
public class HeadersContext {


    public static ThreadLocal<Map<String, String>> headers = new InheritableThreadLocal<>();

    public static final String ORIGIN = "origin";
    public static final String REFERER = "referer";

    public static String getHeader(String key) {
        if (headers.get() != null) {
            String v = headers.get().get(key);
            log.debug("[getHeader] {} : {}", key, v);
            return v;
        }
        throw new NotFoundException("missing header : " + key);
    }
}
