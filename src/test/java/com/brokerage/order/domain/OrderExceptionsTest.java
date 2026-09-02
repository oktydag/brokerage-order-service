package com.brokerage.order.domain;

import com.brokerage.order.domain.valueobjects.OrderStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderExceptionsTest {

    @Test
    void notFoundCarriesAStableCode() {
        UUID id = UUID.randomUUID();
        OrderNotFoundException exception = new OrderNotFoundException(id);

        assertThat(exception.code()).isEqualTo("ORDER_NOT_FOUND");
        assertThat(exception).hasMessageContaining(id.toString());
    }

    @Test
    void illegalTransitionNamesBothStates() {
        IllegalOrderTransitionException exception =
                new IllegalOrderTransitionException(UUID.randomUUID(), OrderStatus.CANCELED, OrderStatus.MATCHED);

        assertThat(exception.code()).isEqualTo("ILLEGAL_ORDER_TRANSITION");
        assertThat(exception).hasMessageContaining("CANCELED").hasMessageContaining("MATCHED");
    }

    @Test
    void invalidOrderCarriesAStableCode() {
        assertThat(new InvalidOrderException("bad").code()).isEqualTo("INVALID_ORDER");
    }
}
