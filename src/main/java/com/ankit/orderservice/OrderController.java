package com.ankit.orderservice;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderRepository repository;
    private final OrderEventProducer producer;

    public OrderController(OrderRepository repository, OrderEventProducer producer) {
        this.repository = repository;
        this.producer = producer;
    }

    @PostMapping
    public ResponseEntity<Order> create(@RequestBody Order order) {
        order.setStatus(OrderStatus.CREATED);
        Order saved = repository.save(order);
        producer.sendOrderCreated(saved.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public List<Order> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}