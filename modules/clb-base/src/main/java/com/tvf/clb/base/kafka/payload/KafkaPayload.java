package com.tvf.clb.base.kafka.payload;

import java.time.Instant;
import java.util.Objects;

public class KafkaPayload {
    private final EventTypeEnum eventType;
    private final Object actualPayload;
    private final Instant timeStamp;

    private KafkaPayload(EventTypeEnum eventType, Object actualPayload, Instant timeStamp) {
        this.eventType = eventType;
        this.actualPayload = actualPayload;
        this.timeStamp = timeStamp;
    }

    public EventTypeEnum getEventType() {
        return eventType;
    }

    public Object getActualPayload() {
        return actualPayload;
    }

    public Instant getTimeStamp() {
        return timeStamp;
    }

    public static class Builder {

        private EventTypeEnum eventType;
        private Object actualPayload;

        public Builder eventType(EventTypeEnum key) {
            this.eventType = Objects.requireNonNull(key);
            return this;
        }

        public Builder actualPayload(Object payload) {
            this.actualPayload = Objects.requireNonNull(payload);
            return this;
        }

        public KafkaPayload build() {
            return new KafkaPayload(this.eventType, this.actualPayload, Instant.now());
        }
    }
}
