package com.tvf.clb.base.kafka.payload;

public enum EventTypeEnum {
    GENERIC("generic");
    private final String configKey;

    EventTypeEnum(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return this.configKey;
    }
}
