package com.brokerage.concurrency;

import com.brokerage.asset.domain.InsufficientUsableBalanceException;
import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.common.domain.valueobjects.AssetName;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.order.application.command.PlaceOrderCommand;
import com.brokerage.order.application.command.PlaceOrderHandler;
import com.brokerage.order.domain.valueobjects.OrderSide;
import com.brokerage.support.ConcurrentRuns;
import com.brokerage.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OrderPlacementConcurrencyTest extends IntegrationTestSupport {

    private static final int THREADS = 20;
    private static final long BALANCE = 75_000L;
    private static final long ORDER_COST = 10_000L;
    private static final int AFFORDABLE = (int) (BALANCE / ORDER_COST);

    @Autowired
    private PlaceOrderHandler placeOrder;

    @Test
    void simultaneousOrdersCannotOverdrawTheSameBalance() throws Exception {
        CustomerId customerId = customerWith(Map.of("TRY", BALANCE, "THYAO", 10_000L));
        PlaceOrderCommand command = new PlaceOrderCommand(customerId, AssetName.of("THYAO"),
                OrderSide.BUY, Amount.of(100), Amount.of(100), null);

        List<ConcurrentRuns.Outcome<Object>> outcomes =
                ConcurrentRuns.race(THREADS, () -> placeOrder.handle(command));

        long accepted = outcomes.stream().filter(ConcurrentRuns.Outcome::succeeded).count();
        long refused = outcomes.stream()
                .filter(outcome -> outcome.failure() instanceof InsufficientUsableBalanceException)
                .count();

        assertThat(accepted).isEqualTo(AFFORDABLE);
        assertThat(refused).isEqualTo(THREADS - AFFORDABLE);
        assertThat(sizeOf(customerId, "TRY")).isEqualTo(Amount.of(BALANCE));
        assertThat(usableSizeOf(customerId, "TRY"))
                .isEqualTo(Amount.of(BALANCE - accepted * ORDER_COST));
    }

    @Test
    void simultaneousSellsCannotOverdrawTheSameHolding() throws Exception {
        CustomerId customerId = customerWith(Map.of("TRY", 1_000L, "THYAO", 250L));
        PlaceOrderCommand command = new PlaceOrderCommand(customerId, AssetName.of("THYAO"),
                OrderSide.SELL, Amount.of(100), Amount.of(10), null);

        List<ConcurrentRuns.Outcome<Object>> outcomes =
                ConcurrentRuns.race(THREADS, () -> placeOrder.handle(command));

        long accepted = outcomes.stream().filter(ConcurrentRuns.Outcome::succeeded).count();

        assertThat(accepted).isEqualTo(2);
        assertThat(usableSizeOf(customerId, "THYAO")).isEqualTo(Amount.of(50));
        assertThat(sizeOf(customerId, "THYAO")).isEqualTo(Amount.of(250));
    }

    @Test
    void theUsableBalanceNeverExceedsWhatIsOwned() throws Exception {
        CustomerId customerId = customerWith(Map.of("TRY", BALANCE, "THYAO", 10_000L));
        PlaceOrderCommand command = new PlaceOrderCommand(customerId, AssetName.of("THYAO"),
                OrderSide.BUY, Amount.of(100), Amount.of(100), null);

        ConcurrentRuns.race(THREADS, () -> placeOrder.handle(command));

        Amount size = sizeOf(customerId, "TRY");
        Amount usable = usableSizeOf(customerId, "TRY");
        assertThat(usable.isGreaterThan(size)).isFalse();
        assertThat(usable.toBigDecimal()).isNotNegative();
    }
}
