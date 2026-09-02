package com.brokerage.order.application.command;

import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.common.domain.valueobjects.AssetName;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.common.domain.valueobjects.IdempotencyKey;
import com.brokerage.common.idempotency.DuplicateRequestException;
import com.brokerage.common.idempotency.IdempotencyClaims;
import com.brokerage.order.application.OrderView;
import com.brokerage.order.domain.Order;
import com.brokerage.order.domain.OrderNotFoundException;
import com.brokerage.order.domain.valueobjects.OrderSide;
import com.brokerage.order.infrastructure.OrderQueryRepository;
import com.brokerage.support.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceOrderIdempotencyTest {

    private static final IdempotencyKey KEY = IdempotencyKey.of("K1");

    @Mock
    private IdempotencyClaims claims;
    @Mock
    private OrderQueryRepository orders;

    private PlaceOrderIdempotency idempotency;
    private Order stored;

    @BeforeEach
    void setUp() {
        idempotency = new PlaceOrderIdempotency(claims, orders);
        stored = Fixtures.buyOrder();
    }

    private PlaceOrderCommand command(IdempotencyKey key) {
        return new PlaceOrderCommand(Fixtures.CUSTOMER, Fixtures.THYAO, OrderSide.BUY,
                Amount.of(100), Amount.of(300), key);
    }

    @Test
    void placesTheOrderWhenNoKeyIsSupplied() {
        AtomicInteger invocations = new AtomicInteger();

        PlaceOrderResult result = idempotency.apply(command(null), () -> {
            invocations.incrementAndGet();
            return OrderView.from(stored);
        });

        assertThat(result.replayed()).isFalse();
        assertThat(invocations).hasValue(1);
        verify(claims, never()).resolve(any(), any(), any());
    }

    @Test
    void placesTheOrderWhenTheKeyHasNotBeenSeen() {
        PlaceOrderCommand command = command(KEY);
        when(claims.resolve(Fixtures.CUSTOMER, KEY, command.fingerprint())).thenReturn(Optional.empty());

        PlaceOrderResult result = idempotency.apply(command, () -> OrderView.from(stored));

        assertThat(result.replayed()).isFalse();
        assertThat(result.order().id()).isEqualTo(stored.getId());
    }

    @Test
    void replaysTheStoredOrderWithoutPlacingAgain() {
        PlaceOrderCommand command = command(KEY);
        when(claims.resolve(Fixtures.CUSTOMER, KEY, command.fingerprint()))
                .thenReturn(Optional.of(stored.getId()));
        when(orders.findById(stored.getId())).thenReturn(Optional.of(stored));
        AtomicInteger invocations = new AtomicInteger();

        PlaceOrderResult result = idempotency.apply(command, () -> {
            invocations.incrementAndGet();
            return OrderView.from(stored);
        });

        assertThat(result.replayed()).isTrue();
        assertThat(result.order().id()).isEqualTo(stored.getId());
        assertThat(invocations).hasValue(0);
    }

    @Test
    void replaysAfterLosingTheRaceToAConcurrentDuplicate() {
        PlaceOrderCommand command = command(KEY);
        when(claims.resolve(Fixtures.CUSTOMER, KEY, command.fingerprint()))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(stored.getId()));
        when(orders.findById(stored.getId())).thenReturn(Optional.of(stored));

        PlaceOrderResult result = idempotency.apply(command, () -> {
            throw new DuplicateRequestException(KEY);
        });

        assertThat(result.replayed()).isTrue();
        assertThat(result.order().id()).isEqualTo(stored.getId());
    }

    @Test
    void surfacesTheDuplicateWhenNoClaimCanBeFound() {
        PlaceOrderCommand command = command(KEY);
        when(claims.resolve(Fixtures.CUSTOMER, KEY, command.fingerprint())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> idempotency.apply(command, () -> {
            throw new DuplicateRequestException(KEY);
        })).isInstanceOf(DuplicateRequestException.class);
    }

    @Test
    void reportsAClaimPointingAtAMissingOrder() {
        PlaceOrderCommand command = command(KEY);
        when(claims.resolve(Fixtures.CUSTOMER, KEY, command.fingerprint()))
                .thenReturn(Optional.of(stored.getId()));
        when(orders.findById(stored.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> idempotency.apply(command, () -> OrderView.from(stored)))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
