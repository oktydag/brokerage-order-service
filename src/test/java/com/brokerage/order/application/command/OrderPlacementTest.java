package com.brokerage.order.application.command;

import com.brokerage.asset.domain.AssetNotHeldException;
import com.brokerage.asset.domain.InsufficientUsableBalanceException;
import com.brokerage.asset.domain.Portfolio;
import com.brokerage.asset.domain.PortfolioRepository;
import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.common.domain.valueobjects.AssetName;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.common.domain.valueobjects.IdempotencyKey;
import com.brokerage.common.idempotency.IdempotencyClaims;
import com.brokerage.order.application.OrderView;
import com.brokerage.order.domain.Order;
import com.brokerage.order.domain.OrderRepository;
import com.brokerage.order.domain.event.OrderPlaced;
import com.brokerage.order.domain.valueobjects.OrderSide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPlacementTest {

    private static final CustomerId CUSTOMER = CustomerId.of("CUST-1");
    private static final AssetName THYAO = AssetName.of("THYAO");
    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    @Mock
    private OrderRepository orders;
    @Mock
    private PortfolioRepository portfolios;
    @Mock
    private IdempotencyClaims claims;
    @Mock
    private ApplicationEventPublisher events;

    private OrderPlacement placement;
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        placement = new OrderPlacement(orders, portfolios, claims, events,
                Clock.fixed(NOW, ZoneOffset.UTC));
        portfolio = Portfolio.empty(CUSTOMER);
        portfolio.deposit(AssetName.TRY, Amount.of(100_000));
        portfolio.deposit(THYAO, Amount.of(500));
    }

    private PlaceOrderCommand buy(IdempotencyKey key) {
        return new PlaceOrderCommand(CUSTOMER, THYAO, OrderSide.BUY,
                Amount.of(100), Amount.of(300), key);
    }

    @Test
    void reservesTheCashLegAndStoresThePendingOrder() {
        when(portfolios.lockForUpdate(CUSTOMER)).thenReturn(portfolio);
        when(orders.save(any(Order.class))).thenAnswer(call -> call.getArgument(0));

        OrderView view = placement.execute(buy(null));

        assertThat(view.status()).isEqualTo("PENDING");
        assertThat(view.totalValue()).isEqualByComparingTo("30000");
        assertThat(view.createDate()).isEqualTo(NOW);
        assertThat(portfolio.holding(AssetName.TRY).orElseThrow().getUsableSize())
                .isEqualTo(Amount.of(70_000));
        verify(portfolios).save(portfolio);
        verify(events).publishEvent(any(OrderPlaced.class));
    }

    @Test
    void reservesTheStockLegForASell() {
        when(portfolios.lockForUpdate(CUSTOMER)).thenReturn(portfolio);
        when(orders.save(any(Order.class))).thenAnswer(call -> call.getArgument(0));

        placement.execute(new PlaceOrderCommand(CUSTOMER, THYAO, OrderSide.SELL,
                Amount.of(50), Amount.of(200), null));

        assertThat(portfolio.holding(THYAO).orElseThrow().getUsableSize()).isEqualTo(Amount.of(450));
        assertThat(portfolio.holding(AssetName.TRY).orElseThrow().getUsableSize())
                .isEqualTo(Amount.of(100_000));
    }

    @Test
    void claimsTheIdempotencyKeyWithTheOrderIdBeforeReserving() {
        when(portfolios.lockForUpdate(CUSTOMER)).thenReturn(portfolio);
        when(orders.save(any(Order.class))).thenAnswer(call -> call.getArgument(0));
        IdempotencyKey key = IdempotencyKey.of("K1");
        PlaceOrderCommand command = buy(key);

        OrderView view = placement.execute(command);

        verify(claims).claim(eq(CUSTOMER), eq(key), eq(command.fingerprint()), eq(view.id()));
    }

    @Test
    void passesNoKeyThroughWhenTheClientSentNone() {
        when(portfolios.lockForUpdate(CUSTOMER)).thenReturn(portfolio);
        when(orders.save(any(Order.class))).thenAnswer(call -> call.getArgument(0));

        placement.execute(buy(null));

        verify(claims).claim(eq(CUSTOMER), isNull(), any(), any());
    }

    @Test
    void rejectsAnOrderThatOverdrawsTheUsableBalance() {
        when(portfolios.lockForUpdate(CUSTOMER)).thenReturn(portfolio);

        assertThatThrownBy(() -> placement.execute(new PlaceOrderCommand(CUSTOMER, THYAO,
                OrderSide.BUY, Amount.of(10_000), Amount.of(300), null)))
                .isInstanceOf(InsufficientUsableBalanceException.class);

        verify(orders, never()).save(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    void rejectsAnOrderForAnAssetTheCustomerDoesNotHold() {
        when(portfolios.lockForUpdate(CUSTOMER)).thenReturn(portfolio);

        assertThatThrownBy(() -> placement.execute(new PlaceOrderCommand(CUSTOMER,
                AssetName.of("GARAN"), OrderSide.SELL, Amount.of(1), Amount.of(1), null)))
                .isInstanceOf(AssetNotHeldException.class);

        verify(orders, never()).save(any());
    }

    @Test
    void publishesAnEventDescribingThePlacedOrder() {
        when(portfolios.lockForUpdate(CUSTOMER)).thenReturn(portfolio);
        when(orders.save(any(Order.class))).thenAnswer(call -> call.getArgument(0));

        OrderView view = placement.execute(buy(null));

        ArgumentCaptor<OrderPlaced> captor = ArgumentCaptor.forClass(OrderPlaced.class);
        verify(events).publishEvent(captor.capture());
        OrderPlaced event = captor.getValue();
        assertThat(event.orderId()).isEqualTo(view.id());
        assertThat(event.customerId()).isEqualTo("CUST-1");
        assertThat(event.assetName()).isEqualTo("THYAO");
        assertThat(event.orderSide()).isEqualTo("BUY");
        assertThat(event.occurredAt()).isEqualTo(NOW);
    }
}
