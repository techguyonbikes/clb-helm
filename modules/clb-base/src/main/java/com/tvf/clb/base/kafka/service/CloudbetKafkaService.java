package com.tvf.clb.base.kafka.service;

import com.google.gson.Gson;
import com.tvf.clb.base.exception.NotFoundException;
import com.tvf.clb.base.kafka.payload.EventTypeEnum;
import com.tvf.clb.base.kafka.payload.KafkaPayload;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;

@Component
@Slf4j
@ConfigurationProperties(prefix = "spring.kafka")
public class CloudbetKafkaService {
    private Map<String, String> topics;
    private final KafkaProducer<Object, Object> producer;

    public CloudbetKafkaService(KafkaProducer<Object, Object> producer) {
        this.producer = producer;
    }

    public Future<org.apache.kafka.clients.producer.RecordMetadata> publishKafka(KafkaPayload payload, String key, Callback callback) {
        String topic = resolveTopic(payload.getEventType());
        String jsonPayload = new Gson().toJson(payload.getActualPayload());
        final ProducerRecord<Object, Object> producerRecord = new ProducerRecord<>(topic, key, jsonPayload);
        return producer.send(producerRecord, callback);
    }

    private String resolveTopic(EventTypeEnum eventTypeEnum) {
        return Optional.ofNullable(topics.get(eventTypeEnum.getConfigKey())).orElseThrow(() -> new NotFoundException("Cannot find topic configured for this event type"));
    }

    public void setTopics(Map<String, String> topics) {
        this.topics = topics;
    }


}
