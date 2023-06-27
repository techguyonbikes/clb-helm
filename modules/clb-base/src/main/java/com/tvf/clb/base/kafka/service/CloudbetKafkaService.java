package com.tvf.clb.base.kafka.service;

import com.tvf.clb.base.exception.NotFoundException;
import com.tvf.clb.base.kafka.payload.EventTypeEnum;
import com.tvf.clb.base.kafka.payload.KafkaPayload;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
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
        final ProducerRecord<Object, Object> producerRecord = new ProducerRecord<>(topic, key, payload.getPayload());
        return producer.send(producerRecord, callback);
    }

    public Future<org.apache.kafka.clients.producer.RecordMetadata> publishKafka(KafkaPayload payload, String key,Callback callback) {
        String topic = resolveTopic(payload.getEventType());
        final ProducerRecord<Object, Object> producerRecord = new ProducerRecord<>(topic, key, payload.getPayload());
        return producer.send(producerRecord, callback);
    }

    private String resolveTopic(EventTypeEnum eventTypeEnum) {
        return Optional.ofNullable(topics.get(eventTypeEnum.getConfigKey())).orElseThrow(() -> new NotFoundException("Cannot find topic configured for this event type"));
    }


    @PostConstruct
    public void doTest() {
        log.info(topics.toString());
    }

    public void setTopics(Map<String, String> topics) {
        this.topics = topics;
    }
}
