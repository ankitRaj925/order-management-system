package com.ankit.orderservice;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {

    private final OrderRepository repository;

    public OrderEventConsumer(OrderRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = KafkaTopicConfig.ORDER_CREATED_TOPIC)
    public void handleOrderCreated(String orderId) throws InterruptedException {
        Thread.sleep(3000); // processing ka simulation
        repository.findById(Long.parseLong(orderId)).ifPresent(order -> {
            order.setStatus(OrderStatus.CONFIRMED);
            repository.save(order);
        });
    }
}