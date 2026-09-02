package com.brokerage.concurrency;

import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.common.domain.valueobjects.AssetName;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.common.domain.valueobjects.IdempotencyKey;
import com.brokerage.order.application.command.PlaceOrderCommand;
import com.brokerage.order.application.command.PlaceOrderHandler;
import com.brokerage.order.application.command.PlaceOrderResult;
import com.brokerage.order.domain.valueobjects.OrderSide;
import com.brokerage.support.ConcurrentRuns;
import com.brokerage.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyConcurrencyTest extends IntegrationTestSupport {

    private static final int THREADS = 20;

    @Autowired
    private PlaceOrderHandler placeOrder;

    @Test
    void simultaneousRetriesOfOneRequestProduceASingleOrder() throws Exception {
        CustomerId customerId = customerWith(Map.of("TRY", 100_000L, "THYAO", 500L));
        PlaceOrderCommand command = new PlaceOrderCommand(customerId, AssetName.of("THYAO"),
                OrderSide.BUY, Amount.of(100), Amount.of(300),
                IdempotencyKey.of("K-" + UUID.randomUUID()));

        List<ConcurrentRuns.Outcome<PlaceOrderResult>> outcomes =
                ConcurrentRuns.race(THREADS, () -> placeOrder.handle(command));

        List<PlaceOrderResult> succeeded = outcomes.stream()
                .filter(ConcurrentRuns.Outcome::succeeded)
                .map(ConcurrentRuns.Outcome::value)
                .toList();
        Set<UUID> orderIds = succeeded.stream()
                .map(result -> result.order().id())
                .collect(Collectors.toSet());

        assertThat(succeeded).hasSize(THREADS);
        assertThat(orderIds).hasSize(1);
        assertThat(succeeded.stream().filter(result -> !result.replayed()).count()).isEqualTo(1);
        assertThat(usableSizeOf(customerId, "TRY")).isEqualTo(Amount.of(70_000));
    }

    @Test
    void distinctKeysStillProduceDistinctOrders() throws Exception {
        CustomerId customerId = customerWith(Map.of("TRY", 100_000L, "THYAO", 500L));

        List<ConcurrentRuns.Outcome<PlaceOrderResult>> outcomes = ConcurrentRuns.race(5,
                () -> placeOrder.handle(new PlaceOrderCommand(customerId, AssetName.of("THYAO"),
                        OrderSide.BUY, Amount.of(10), Amount.of(300),
                        IdempotencyKey.of("K-" + UUID.randomUUID()))));

        Set<UUID> orderIds = outcomes.stream()
                .filter(ConcurrentRuns.Outcome::succeeded)
                .map(outcome -> outcome.value().order().id())
                .collect(Collectors.toSet());

        assertThat(orderIds).hasSize(5);
        assertThat(usableSizeOf(customerId, "TRY")).isEqualTo(Amount.of(85_000));
    }
}
