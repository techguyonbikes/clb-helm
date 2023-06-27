package com.tvf.clb.base.kafka.payload;

import java.time.Instant;
import java.util.Objects;

public class KafkaPayload {
    private final EventTypeEnum eventType;
    private final Object payload;
    private final Instant timeStamp;

    private KafkaPayload(EventTypeEnum eventType, Object payload, Instant timeStamp) {
        this.eventType = eventType;
        this.payload = payload;
        this.timeStamp = timeStamp;
    }

    public EventTypeEnum getEventType() {
        return eventType;
    }

    public Object getPayload() {
        return payload;
    }

    public Instant getTimeStamp() {
        return timeStamp;
    }

    public static class Builder {

        private EventTypeEnum eventType;
        private Object payload;

        public Builder eventType(EventTypeEnum key) {
            this.eventType = Objects.requireNonNull(key);
            return this;
        }

        public Builder actualPayload(Object payload) {
            this.payload = Objects.requireNonNull(payload);
            return this;
        }

        public KafkaPayload build() {
            return new KafkaPayload(this.eventType, this.payload, Instant.now());
        }
    }
}
