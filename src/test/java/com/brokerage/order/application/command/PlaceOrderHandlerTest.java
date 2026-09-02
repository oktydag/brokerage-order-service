package com.brokerage.order.application.command;

import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.order.application.OrderView;
import com.brokerage.order.domain.valueobjects.OrderSide;
import com.brokerage.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceOrderHandlerTest {

    @Mock
    private OrderPlacement placement;
    @Mock
    private PlaceOrderIdempotency idempotency;

    @Test
    void routesPlacementThroughTheIdempotencyGuard() {
        PlaceOrderHandler handler = new PlaceOrderHandler(placement, idempotency);
        PlaceOrderCommand command = new PlaceOrderCommand(Fixtures.CUSTOMER, Fixtures.THYAO,
                OrderSide.BUY, Amount.of(100), Amount.of(300), null);
        OrderView view = OrderView.from(Fixtures.buyOrder());
        when(placement.execute(command)).thenReturn(view);
        when(idempotency.apply(eq(command), any()))
                .thenAnswer(call -> new PlaceOrderResult(
                        call.<Supplier<OrderView>>getArgument(1).get(), false));

        PlaceOrderResult result = handler.handle(command);

        assertThat(result.order()).isEqualTo(view);
        assertThat(result.replayed()).isFalse();
    }
}
