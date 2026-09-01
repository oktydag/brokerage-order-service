package com.brokerage.order.application.command;

import com.brokerage.common.application.CommandHandler;
import org.springframework.stereotype.Component;

@Component
public class PlaceOrderHandler implements CommandHandler<PlaceOrderCommand, PlaceOrderResult> {

    private final OrderPlacement placement;
    private final PlaceOrderIdempotency idempotency;

    public PlaceOrderHandler(OrderPlacement placement, PlaceOrderIdempotency idempotency) {
        this.placement = placement;
        this.idempotency = idempotency;
    }

    @Override
    public PlaceOrderResult handle(PlaceOrderCommand command) {
        return idempotency.apply(command, () -> placement.execute(command));
    }
}
