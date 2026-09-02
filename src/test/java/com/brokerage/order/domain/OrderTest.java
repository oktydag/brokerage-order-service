package com.brokerage.order.domain;

import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.common.domain.valueobjects.AssetName;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.common.domain.valueobjects.Reservation;
import com.brokerage.common.domain.valueobjects.Settlement;
import com.brokerage.order.domain.valueobjects.OrderSide;
import com.brokerage.order.domain.valueobjects.OrderStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private static final CustomerId CUSTOMER = CustomerId.of("CUST-1");
    private static final AssetName THYAO = AssetName.of("THYAO");
    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    private Order buy() {
        return Order.place(CUSTOMER, THYAO, OrderSide.BUY, Amount.of(100), Amount.of(300), NOW);
    }

    private Order sell() {
        return Order.place(CUSTOMER, THYAO, OrderSide.SELL, Amount.of(50), Amount.of(200), NOW);
    }

    @Test
    void isCreatedPending() {
        Order order = buy();

        assertThat(order.getId()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.isPending()).isTrue();
        assertThat(order.getCustomerId()).isEqualTo(CUSTOMER);
        assertThat(order.getAssetName()).isEqualTo(THYAO);
        assertThat(order.getOrderSide()).isEqualTo(OrderSide.BUY);
        assertThat(order.getSize()).isEqualTo(Amount.of(100));
        assertThat(order.getPrice()).isEqualTo(Amount.of(300));
        assertThat(order.getCreateDate()).isEqualTo(NOW);
    }

    @Test
    void valuesItselfAtSizeTimesPrice() {
        assertThat(buy().totalValue()).isEqualTo(Amount.of(30_000));
    }

    @Test
    void refusesToTradeTheSettlementCurrencyAgainstItself() {
        assertThatThrownBy(() -> Order.place(CUSTOMER, AssetName.TRY, OrderSide.BUY,
                Amount.of(1), Amount.of(1), NOW))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("settlement currency");
    }

    @Test
    void refusesNonPositiveSizeOrPrice() {
        assertThatThrownBy(() -> Order.place(CUSTOMER, THYAO, OrderSide.BUY,
                Amount.ZERO, Amount.of(1), NOW))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("size");

        assertThatThrownBy(() -> Order.place(CUSTOMER, THYAO, OrderSide.BUY,
                Amount.of(1), Amount.ZERO, NOW))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("price");
    }

    @Test
    void aBuyReservesCashAndReceivesStock() {
        Order order = buy();

        assertThat(order.reservation()).isEqualTo(new Reservation(AssetName.TRY, Amount.of(30_000)));
        assertThat(order.settlement()).isEqualTo(new Settlement(
                new Reservation(AssetName.TRY, Amount.of(30_000)),
                new Reservation(THYAO, Amount.of(100))));
    }

    @Test
    void aSellReservesStockAndReceivesCash() {
        Order order = sell();

        assertThat(order.reservation()).isEqualTo(new Reservation(THYAO, Amount.of(50)));
        assertThat(order.settlement()).isEqualTo(new Settlement(
                new Reservation(THYAO, Amount.of(50)),
                new Reservation(AssetName.TRY, Amount.of(10_000))));
    }

    @Test
    void theSettledOutgoingLegAlwaysEqualsWhatWasReserved() {
        assertThat(buy().settlement().outgoing()).isEqualTo(buy().reservation());
        assertThat(sell().settlement().outgoing()).isEqualTo(sell().reservation());
    }

    @Test
    void cancelReportsTheReservationToRelease() {
        Order order = buy();

        Optional<Reservation> released = order.cancel();

        assertThat(released).contains(new Reservation(AssetName.TRY, Amount.of(30_000)));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    void cancellingAnAlreadyCancelledOrderReleasesNothingAgain() {
        Order order = buy();
        order.cancel();

        assertThat(order.cancel()).isEmpty();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    void matchReportsTheSettlementToApply() {
        Order order = buy();

        Optional<Settlement> settlement = order.match();

        assertThat(settlement).contains(order.settlement());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.MATCHED);
    }

    @Test
    void matchingAnAlreadyMatchedOrderSettlesNothingAgain() {
        Order order = buy();
        order.match();

        assertThat(order.match()).isEmpty();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.MATCHED);
    }

    @Test
    void aMatchedOrderCannotBeCancelled() {
        Order order = buy();
        order.match();

        assertThatThrownBy(order::cancel)
                .isInstanceOf(IllegalOrderTransitionException.class)
                .hasMessageContaining("MATCHED")
                .hasMessageContaining("CANCELED");
    }

    @Test
    void aCancelledOrderCannotBeMatched() {
        Order order = buy();
        order.cancel();

        assertThatThrownBy(order::match)
                .isInstanceOf(IllegalOrderTransitionException.class);
    }

    @Test
    void knowsWhoItBelongsTo() {
        Order order = buy();

        assertThat(order.belongsTo(CUSTOMER)).isTrue();
        assertThat(order.belongsTo(CustomerId.of("CUST-2"))).isFalse();
    }
}
