package com.brokerage.concurrency;

import com.brokerage.common.domain.valueobjects.AccessScope;
import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.common.domain.valueobjects.AssetName;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.matching.application.command.MatchOrderCommand;
import com.brokerage.matching.application.command.MatchOrderHandler;
import com.brokerage.matching.application.command.MatchOrderResult;
import com.brokerage.order.application.OrderView;
import com.brokerage.order.application.command.CancelOrderCommand;
import com.brokerage.order.application.command.CancelOrderHandler;
import com.brokerage.order.application.command.PlaceOrderCommand;
import com.brokerage.order.application.command.PlaceOrderHandler;
import com.brokerage.order.domain.valueobjects.OrderSide;
import com.brokerage.support.ConcurrentRuns;
import com.brokerage.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderLifecycleConcurrencyTest extends IntegrationTestSupport {

    private static final int THREADS = 16;

    @Autowired
    private PlaceOrderHandler placeOrder;

    @Autowired
    private CancelOrderHandler cancelOrder;

    @Autowired
    private MatchOrderHandler matchOrder;

    private UUID placeBuy(CustomerId customerId) {
        return placeOrder.handle(new PlaceOrderCommand(customerId, AssetName.of("THYAO"),
                OrderSide.BUY, Amount.of(100), Amount.of(300), null)).order().id();
    }

    @Test
    void simultaneousCancellationsReleaseTheReservationExactlyOnce() throws Exception {
        CustomerId customerId = customerWith(Map.of("TRY", 100_000L, "THYAO", 500L));
        UUID orderId = placeBuy(customerId);
        CancelOrderCommand command = new CancelOrderCommand(orderId, AccessScope.unrestricted());

        List<ConcurrentRuns.Outcome<OrderView>> outcomes =
                ConcurrentRuns.race(THREADS, () -> cancelOrder.handle(command));

        assertThat(outcomes).allMatch(ConcurrentRuns.Outcome::succeeded);
        assertThat(outcomes).allMatch(outcome -> "CANCELED".equals(outcome.value().status()));
        assertThat(usableSizeOf(customerId, "TRY")).isEqualTo(Amount.of(100_000));
        assertThat(sizeOf(customerId, "TRY")).isEqualTo(Amount.of(100_000));
    }

    @Test
    void simultaneousMatchesSettleTheOrderExactlyOnce() throws Exception {
        CustomerId customerId = customerWith(Map.of("TRY", 100_000L, "THYAO", 500L));
        UUID orderId = placeBuy(customerId);
        MatchOrderCommand command = new MatchOrderCommand(orderId);

        List<ConcurrentRuns.Outcome<MatchOrderResult>> outcomes =
                ConcurrentRuns.race(THREADS, () -> matchOrder.handle(command));

        long applied = outcomes.stream()
                .filter(ConcurrentRuns.Outcome::succeeded)
                .filter(outcome -> outcome.value().applied())
                .count();

        assertThat(applied).isEqualTo(1);
        assertThat(sizeOf(customerId, "TRY")).isEqualTo(Amount.of(70_000));
        assertThat(usableSizeOf(customerId, "TRY")).isEqualTo(Amount.of(70_000));
        assertThat(sizeOf(customerId, "THYAO")).isEqualTo(Amount.of(600));
        assertThat(usableSizeOf(customerId, "THYAO")).isEqualTo(Amount.of(600));
    }

    @Test
    void aCancellationRacingAMatchLeavesExactlyOneTerminalState() throws Exception {
        CustomerId customerId = customerWith(Map.of("TRY", 100_000L, "THYAO", 500L));
        UUID orderId = placeBuy(customerId);

        List<ConcurrentRuns.Outcome<String>> outcomes = ConcurrentRuns.race(THREADS, () -> {
            if (Thread.currentThread().hashCode() % 2 == 0) {
                return cancelOrder.handle(new CancelOrderCommand(orderId, AccessScope.unrestricted()))
                        .status();
            }
            return matchOrder.handle(new MatchOrderCommand(orderId)).order().status();
        });

        assertThat(outcomes).anyMatch(ConcurrentRuns.Outcome::succeeded);

        Amount cash = sizeOf(customerId, "TRY");
        Amount usableCash = usableSizeOf(customerId, "TRY");
        assertThat(usableCash.isGreaterThan(cash)).isFalse();
        assertThat(cash).isIn(Amount.of(100_000), Amount.of(70_000));
        assertThat(usableCash).isEqualTo(cash);
    }
}
