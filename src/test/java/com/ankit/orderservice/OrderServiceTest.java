package com.ankit.orderservice;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private final OrderRepository repository = mock(OrderRepository.class);
    private final OrderService service = new OrderService(repository);

    private Order order(Long id, OrderStatus status) {
        Order o = new Order();
        o.setId(id);
        o.setStatus(status);
        return o;
    }

    @Test
    void shipMovesConfirmedOrderToShipped() {
        Order o = order(1L, OrderStatus.CONFIRMED);
        when(repository.findById(1L)).thenReturn(Optional.of(o));
        when(repository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.ship(1L);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void shipRejectsCreatedOrder() {
        when(repository.findById(1L)).thenReturn(Optional.of(order(1L, OrderStatus.CREATED)));

        assertThatThrownBy(() -> service.ship(1L))
                .isInstanceOf(OrderExceptions.InvalidTransition.class);
        verify(repository, never()).save(any());
    }

    @Test
    void repeatedShipRequestIsRejected() {
        when(repository.findById(1L)).thenReturn(Optional.of(order(1L, OrderStatus.SHIPPED)));

        assertThatThrownBy(() -> service.ship(1L))
                .isInstanceOf(OrderExceptions.InvalidTransition.class);
    }

    @Test
    void shipMissingOrderThrowsNotFound() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ship(9L))
                .isInstanceOf(OrderExceptions.NotFound.class);
    }

    @Test
    void confirmMovesCreatedOrderToConfirmed() {
        Order o = order(1L, OrderStatus.CREATED);
        when(repository.findById(1L)).thenReturn(Optional.of(o));

        service.confirm(1L);

        assertThat(o.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(repository).save(o);
    }

    @Test
    void confirmNeverMovesShippedOrderBackwards() {
        Order o = order(1L, OrderStatus.SHIPPED);
        when(repository.findById(1L)).thenReturn(Optional.of(o));

        service.confirm(1L);

        assertThat(o.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        verify(repository, never()).save(any());
    }

    @Test
    void deleteMissingOrderThrowsNotFound() {
        when(repository.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(7L))
                .isInstanceOf(OrderExceptions.NotFound.class);
        verify(repository, never()).deleteById(any());
    }

    @Test
    void deleteRemovesExistingOrder() {
        when(repository.existsById(5L)).thenReturn(true);

        service.delete(5L);

        verify(repository).deleteById(5L);
    }
}