package com.ankit.orderservice;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OrderControllerTest {

    private final OrderRepository repository = mock(OrderRepository.class);
    private final OrderEventProducer producer = mock(OrderEventProducer.class);
    private final OrderController controller = new OrderController(repository, producer);

    @Test
    void createOrderReturnsCreatedAndSendsKafkaEvent() {
        Order input = new Order();
        input.setCustomerName("Ankit");
        input.setProductName("Laptop");
        input.setQuantity(1);
        input.setPrice(55000.0);

        Order saved = new Order();
        saved.setId(1L);
        saved.setStatus(OrderStatus.CREATED);
        when(repository.save(any(Order.class))).thenReturn(saved);

        ResponseEntity<Order> response = controller.create(input);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getStatus()).isEqualTo(OrderStatus.CREATED);
        verify(producer).sendOrderCreated(1L);
    }

    @Test
    void getByIdReturnsNotFoundWhenOrderMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<Order> response = controller.getById(99L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void newOrderDefaultsToCreatedStatus() {
        assertThat(new Order().getStatus()).isEqualTo(OrderStatus.CREATED);
    }
}