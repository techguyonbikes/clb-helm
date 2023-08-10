package com.tvf.clb.base.kafka.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.tvf.clb.base.exception.NotFoundException;
import com.tvf.clb.base.kafka.config.KafkaDebounceBufferPool;
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
import java.util.UUID;
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

    public Future<org.apache.kafka.clients.producer.RecordMetadata> publishKafka(KafkaPayload payload, Callback callback) {
        String topic = resolveTopic(payload.getEventType());
        String key = UUID.randomUUID().toString();
        final ProducerRecord<Object, Object> producerRecord = new ProducerRecord<>(topic, key, payload.getActualPayload());
        return producer.send(producerRecord, callback);
    }

    public Future<org.apache.kafka.clients.producer.RecordMetadata> publishKafka(KafkaPayload payload, String key, Callback callback) {
        String topic = resolveTopic(payload.getEventType());
        Future<org.apache.kafka.clients.producer.RecordMetadata> result = null;
        try {
            result = sendUnique(payload, topic, key, callback);
        } catch (JsonProcessingException e) {
            log.warn("Cannot send Kafka Message : {}", payload.getActualPayload());
        }

        return result;
    }

    private String resolveTopic(EventTypeEnum eventTypeEnum) {
        return Optional.ofNullable(topics.get(eventTypeEnum.getConfigKey())).orElseThrow(() -> new NotFoundException("Cannot find topic configured for this event type"));
    }

    private Future<org.apache.kafka.clients.producer.RecordMetadata> sendUnique(KafkaPayload payload, String topic, String key, Callback callback) throws JsonProcessingException {
        Object sentMessage = KafkaDebounceBufferPool.bufferPool()
                .get(payload.getActualPayload().getClass());
        if (sentMessage != null) {
            ObjectMapper mapper = new ObjectMapper();
            String sent = mapper.writeValueAsString(sentMessage);
            String incoming = mapper.writeValueAsString(payload.getActualPayload());
            if (mapper.readTree(sent).equals(mapper.readTree(incoming))) {
                return null;
            }
        }

        KafkaDebounceBufferPool.bufferPool().put(payload.getActualPayload().getClass(), payload.getActualPayload());
        String jsonPayload = new Gson().toJson(payload.getActualPayload());
        final ProducerRecord<Object, Object> producerRecord = new ProducerRecord<>(topic, key, jsonPayload);
        return producer.send(producerRecord, callback);
    }

}
