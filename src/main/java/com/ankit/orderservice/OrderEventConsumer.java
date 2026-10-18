package com.ankit.orderservice;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {

    private final OrderService service;

    public OrderEventConsumer(OrderService service) {
        this.service = service;
    }

    @KafkaListener(topics = KafkaTopicConfig.ORDER_CREATED_TOPIC)
    public void handleOrderCreated(String orderId) throws InterruptedException {
        Thread.sleep(3000); // processing ka simulation
        Long id;
        try {
            id = Long.parseLong(orderId.trim());
        } catch (NumberFormatException e) {
            return;
        }
        service.confirm(id);
    }
}