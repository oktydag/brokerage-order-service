package com.brokerage.order.application.command;

import com.brokerage.asset.domain.Portfolio;
import com.brokerage.asset.domain.PortfolioRepository;
import com.brokerage.common.idempotency.IdempotencyClaims;
import com.brokerage.order.application.OrderView;
import com.brokerage.order.domain.Order;
import com.brokerage.order.domain.OrderRepository;
import com.brokerage.order.domain.event.OrderPlaced;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Component
public class OrderPlacement {

    private final OrderRepository orders;
    private final PortfolioRepository portfolios;
    private final IdempotencyClaims claims;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public OrderPlacement(OrderRepository orders, PortfolioRepository portfolios,
                          IdempotencyClaims claims, ApplicationEventPublisher events, Clock clock) {
        this.orders = orders;
        this.portfolios = portfolios;
        this.claims = claims;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public OrderView execute(PlaceOrderCommand command) {
        Order order = Order.place(
                command.customerId(),
                command.assetName(),
                command.orderSide(),
                command.size(),
                command.price(),
                clock.instant());

        claims.claim(command.customerId(), command.idempotencyKey(),
                command.fingerprint(), order.getId());

        Portfolio portfolio = portfolios.lockForUpdate(command.customerId());
        portfolio.reserve(order.reservation());
        portfolios.save(portfolio);

        Order placed = orders.save(order);
        events.publishEvent(OrderPlaced.of(placed));
        return OrderView.from(placed);
    }
}
