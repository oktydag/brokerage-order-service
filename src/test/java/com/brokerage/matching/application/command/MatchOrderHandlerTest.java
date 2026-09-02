package com.brokerage.matching.application.command;

import com.brokerage.asset.domain.Portfolio;
import com.brokerage.asset.domain.PortfolioRepository;
import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.common.domain.valueobjects.AssetName;
import com.brokerage.order.domain.IllegalOrderTransitionException;
import com.brokerage.order.domain.Order;
import com.brokerage.order.domain.OrderNotFoundException;
import com.brokerage.order.domain.OrderRepository;
import com.brokerage.order.domain.event.OrderMatched;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchOrderHandlerTest {

    @Mock
    private OrderRepository orders;
    @Mock
    private PortfolioRepository portfolios;
    @Mock
    private ApplicationEventPublisher events;

    private MatchOrderHandler handler;
    private Portfolio portfolio;
    private Order order;

    @BeforeEach
    void setUp() {
        handler = new MatchOrderHandler(orders, portfolios, events);
        portfolio = Portfolio.empty(Fixtures.CUSTOMER);
        portfolio.deposit(AssetName.TRY, Amount.of(100_000));
        portfolio.deposit(Fixtures.THYAO, Amount.of(500));
        order = Fixtures.buyOrder();
        portfolio.reserve(order.reservation());
    }

    private void givenTheOrderIsStored() {
        when(orders.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(portfolios.lockForUpdate(Fixtures.CUSTOMER)).thenReturn(portfolio);
    }

    @Test
    void settlesBothLegsOfABuy() {
        givenTheOrderIsStored();

        MatchOrderResult result = handler.handle(new MatchOrderCommand(order.getId()));

        assertThat(result.applied()).isTrue();
        assertThat(result.order().status()).isEqualTo("MATCHED");
        assertThat(portfolio.holding(AssetName.TRY).orElseThrow().getSize()).isEqualTo(Amount.of(70_000));
        assertThat(portfolio.holding(AssetName.TRY).orElseThrow().getUsableSize()).isEqualTo(Amount.of(70_000));
        assertThat(portfolio.holding(Fixtures.THYAO).orElseThrow().getSize()).isEqualTo(Amount.of(600));
        verify(portfolios).save(portfolio);
        verify(events).publishEvent(any(OrderMatched.class));
    }

    @Test
    void aRetriedMatchSettlesNothingASecondTime() {
        givenTheOrderIsStored();

        handler.handle(new MatchOrderCommand(order.getId()));
        MatchOrderResult replay = handler.handle(new MatchOrderCommand(order.getId()));

        assertThat(replay.applied()).isFalse();
        assertThat(replay.order().status()).isEqualTo("MATCHED");
        assertThat(portfolio.holding(AssetName.TRY).orElseThrow().getSize()).isEqualTo(Amount.of(70_000));
        verify(events, times(1)).publishEvent(any(OrderMatched.class));
    }

    @Test
    void refusesToMatchACancelledOrder() {
        order.cancel();
        givenTheOrderIsStored();

        assertThatThrownBy(() -> handler.handle(new MatchOrderCommand(order.getId())))
                .isInstanceOf(IllegalOrderTransitionException.class);
    }

    @Test
    void reportsAnUnknownOrder() {
        UUID unknown = UUID.randomUUID();
        when(orders.findByIdForUpdate(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(new MatchOrderCommand(unknown)))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
