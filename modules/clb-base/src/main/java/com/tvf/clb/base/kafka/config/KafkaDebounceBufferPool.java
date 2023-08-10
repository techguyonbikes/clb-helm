package com.tvf.clb.base.kafka.config;

import java.util.HashMap;
import java.util.Map;


public final class KafkaDebounceBufferPool {
    private static volatile KafkaDebounceBufferPool INSTANCE;
    private static final Object mutex = new Object();


    private final HashMap<Class<?>, Object> bufferPool = new HashMap<>();

    private KafkaDebounceBufferPool() {

    }

    private static KafkaDebounceBufferPool instance() {
        KafkaDebounceBufferPool instance = INSTANCE;
        if (instance == null) {
            synchronized (mutex) {
                instance = INSTANCE;
                if (instance == null) {
                    INSTANCE = instance = new KafkaDebounceBufferPool();
                }
            }
        }
        return instance;
    }

    public static Map<Class<?>, Object> bufferPool() {
        return instance().bufferPool;
    }
}
