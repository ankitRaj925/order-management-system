package com.ankit.orderservice;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;
    private String productName;
    private Integer quantity;
    private Double price;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.CREATED;

    private LocalDateTime createdAt = LocalDateTime.now();
}