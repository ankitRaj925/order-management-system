package com.ankit.orderservice;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Customer name is required")
    @Size(max = 50, message = "Customer name can be at most 50 characters")
    private String customerName;

    @NotBlank(message = "Product name is required")
    @Size(max = 100, message = "Product name can be at most 100 characters")
    private String productName;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 1000, message = "Quantity can be at most 1000")
    private Integer quantity;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
    @Digits(integer = 7, fraction = 2, message = "Price can have at most 7 digits before and 2 digits after the decimal point")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.CREATED;

    private LocalDateTime createdAt = LocalDateTime.now();
}