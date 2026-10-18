package com.ankit.orderservice;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OrderControllerTest {
    private final OrderRepository repository = mock(OrderRepository.class);
    private final OrderEventProducer producer = mock(OrderEventProducer.class);
    private final OrderService service = mock(OrderService.class);
    private final AdminSessionService sessions = mock(AdminSessionService.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final OrderController controller = new OrderController(repository, producer, service, sessions);

    @Test void createOrderReturnsCreatedAndSendsKafkaEvent() {
        Order input = new Order(); input.setCustomerName("Ankit"); input.setProductName("Laptop");
        input.setQuantity(1); input.setPrice(new BigDecimal("55000.00"));
        Order saved = new Order(); saved.setId(1L); saved.setStatus(OrderStatus.CREATED);
        when(repository.save(any(Order.class))).thenReturn(saved);
        ResponseEntity<Order> response = controller.create(input);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getStatus()).isEqualTo(OrderStatus.CREATED);
        verify(producer).sendOrderCreated(1L);
    }

    @Test void getByIdReturnsNotFoundWhenOrderMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThat(controller.getById(99L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test void newOrderDefaultsToCreatedStatus() { assertThat(new Order().getStatus()).isEqualTo(OrderStatus.CREATED); }

    @Test void shipRequiresSessionAndThenShips() {
        Order shipped = new Order(); shipped.setId(3L); shipped.setStatus(OrderStatus.SHIPPED);
        when(service.ship(3L)).thenReturn(shipped);
        Order result = controller.ship(3L, request, "csrf");
        verify(sessions).requireAdmin(null, "csrf");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test void deleteRequiresSessionAndReturnsNoContent() {
        ResponseEntity<Void> response = controller.delete(4L, request, "csrf");
        verify(sessions).requireAdmin(null, "csrf"); verify(service).delete(4L);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test void bulkShipChecksSessionAndReturnsPerOrderResult() {
        Order shipped = new Order(); shipped.setId(1L); shipped.setStatus(OrderStatus.SHIPPED);
        when(service.ship(1L)).thenReturn(shipped);
        var response = controller.shipBulk(new OrderController.BulkRequest(java.util.List.of(1L)), request, "csrf");
        verify(sessions).requireAdmin(null, "csrf");
        assertThat(response.get("succeeded")).isEqualTo(1);
    }
}
