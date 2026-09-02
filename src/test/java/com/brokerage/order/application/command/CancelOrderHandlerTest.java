package com.brokerage.order.application.command;

import com.brokerage.asset.domain.Portfolio;
import com.brokerage.asset.domain.PortfolioRepository;
import com.brokerage.common.domain.ForbiddenException;
import com.brokerage.common.domain.valueobjects.AccessScope;
import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.common.domain.valueobjects.AssetName;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.order.application.OrderView;
import com.brokerage.order.domain.IllegalOrderTransitionException;
import com.brokerage.order.domain.Order;
import com.brokerage.order.domain.OrderNotFoundException;
import com.brokerage.order.domain.OrderRepository;
import com.brokerage.order.domain.event.OrderCanceled;
import com.brokerage.support.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelOrderHandlerTest {

    @Mock
    private OrderRepository orders;
    @Mock
    private PortfolioRepository portfolios;
    @Mock
    private ApplicationEventPublisher events;

    private CancelOrderHandler handler;
    private Portfolio portfolio;
    private Order order;

    @BeforeEach
    void setUp() {
        handler = new CancelOrderHandler(orders, portfolios, events);
        portfolio = Portfolio.empty(Fixtures.CUSTOMER);
        portfolio.deposit(AssetName.TRY, Amount.of(100_000));
        order = Fixtures.buyOrder();
    }

    private void givenTheOrderIsStored() {
        when(orders.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(portfolios.lockForUpdate(Fixtures.CUSTOMER)).thenReturn(portfolio);
    }

    @Test
    void releasesTheReservationAndPublishesTheCancellation() {
        portfolio.reserve(order.reservation());
        givenTheOrderIsStored();

        OrderView view = handler.handle(new CancelOrderCommand(order.getId(), AccessScope.unrestricted()));

        assertThat(view.status()).isEqualTo("CANCELED");
        assertThat(portfolio.holding(AssetName.TRY).orElseThrow().getUsableSize())
                .isEqualTo(Amount.of(100_000));
        verify(events).publishEvent(any(OrderCanceled.class));
    }

    @Test
    void aRetriedCancellationReleasesNothingASecondTime() {
        portfolio.reserve(order.reservation());
        givenTheOrderIsStored();

        handler.handle(new CancelOrderCommand(order.getId(), AccessScope.unrestricted()));
        OrderView view = handler.handle(new CancelOrderCommand(order.getId(), AccessScope.unrestricted()));

        assertThat(view.status()).isEqualTo("CANCELED");
        assertThat(portfolio.holding(AssetName.TRY).orElseThrow().getUsableSize())
                .isEqualTo(Amount.of(100_000));
        verify(events, times(1)).publishEvent(any(OrderCanceled.class));
    }

    @Test
    void refusesToCancelAMatchedOrder() {
        portfolio.reserve(order.reservation());
        order.match();
        when(orders.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(portfolios.lockForUpdate(Fixtures.CUSTOMER)).thenReturn(portfolio);

        assertThatThrownBy(() -> handler.handle(
                new CancelOrderCommand(order.getId(), AccessScope.unrestricted())))
                .isInstanceOf(IllegalOrderTransitionException.class);
    }

    @Test
    void reportsAnUnknownOrder() {
        UUID unknown = UUID.randomUUID();
        when(orders.findByIdForUpdate(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(
                new CancelOrderCommand(unknown, AccessScope.unrestricted())))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void refusesToCancelAnotherCustomersOrder() {
        when(orders.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> handler.handle(new CancelOrderCommand(order.getId(),
                AccessScope.of(CustomerId.of("CUST-2")))))
                .isInstanceOf(ForbiddenException.class);

        verify(portfolios, never()).lockForUpdate(any());
    }
}
