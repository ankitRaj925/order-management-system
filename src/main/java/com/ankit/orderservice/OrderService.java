package com.ankit.orderservice;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Order ship(Long id) {
        Order order = repository.findById(id)
                .orElseThrow(() -> new OrderExceptions.NotFound(id));
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new OrderExceptions.InvalidTransition(
                    "Order #" + id + " is " + order.getStatus()
                            + " and cannot be shipped. Only CONFIRMED orders can be shipped.");
        }
        order.setStatus(OrderStatus.SHIPPED);
        return repository.save(order);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new OrderExceptions.NotFound(id);
        }
        repository.deleteById(id);
    }

    @Transactional
    public void confirm(Long id) {
        repository.findById(id).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.CREATED) {
                order.setStatus(OrderStatus.CONFIRMED);
                repository.save(order);
            }
        });
    }
}