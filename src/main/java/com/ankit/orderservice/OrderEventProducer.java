package com.ankit.orderservice;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendOrderCreated(Long orderId) {
        kafkaTemplate.send(KafkaTopicConfig.ORDER_CREATED_TOPIC, String.valueOf(orderId));
    }
}