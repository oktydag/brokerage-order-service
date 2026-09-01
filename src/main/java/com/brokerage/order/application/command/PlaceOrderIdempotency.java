package com.brokerage.order.application.command;

import com.brokerage.common.idempotency.DuplicateRequestException;
import com.brokerage.common.idempotency.IdempotencyClaims;
import com.brokerage.order.application.OrderView;
import com.brokerage.order.domain.OrderNotFoundException;
import com.brokerage.order.infrastructure.OrderQueryRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class PlaceOrderIdempotency {

    private final IdempotencyClaims claims;
    private final OrderQueryRepository orders;

    public PlaceOrderIdempotency(IdempotencyClaims claims, OrderQueryRepository orders) {
        this.claims = claims;
        this.orders = orders;
    }

    public PlaceOrderResult apply(PlaceOrderCommand command, Supplier<OrderView> placement) {
        Optional<OrderView> replayed = replay(command);
        if (replayed.isPresent()) {
            return new PlaceOrderResult(replayed.get(), true);
        }
        try {
            return new PlaceOrderResult(placement.get(), false);
        } catch (DuplicateRequestException e) {
            return new PlaceOrderResult(replay(command).orElseThrow(() -> e), true);
        }
    }

    private Optional<OrderView> replay(PlaceOrderCommand command) {
        if (command.idempotencyKey() == null) {
            return Optional.empty();
        }
        return claims.resolve(command.customerId(), command.idempotencyKey(), command.fingerprint())
                .map(this::load);
    }

    private OrderView load(UUID orderId) {
        return orders.findById(orderId)
                .map(OrderView::from)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
