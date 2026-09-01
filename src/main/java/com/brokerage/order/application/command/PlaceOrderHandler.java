package com.brokerage.order.application.command;

import com.brokerage.asset.domain.Portfolio;
import com.brokerage.asset.domain.PortfolioRepository;
import com.brokerage.common.application.CommandHandler;
import com.brokerage.order.application.OrderView;
import com.brokerage.order.domain.Order;
import com.brokerage.order.domain.OrderRepository;
import com.brokerage.order.domain.event.OrderPlaced;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Component
public class PlaceOrderHandler implements CommandHandler<PlaceOrderCommand, OrderView> {

    private final OrderRepository orders;
    private final PortfolioRepository portfolios;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public PlaceOrderHandler(OrderRepository orders, PortfolioRepository portfolios,
                             ApplicationEventPublisher events, Clock clock) {
        this.orders = orders;
        this.portfolios = portfolios;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public OrderView handle(PlaceOrderCommand command) {
        Order order = Order.place(
                command.customerId(),
                command.assetName(),
                command.orderSide(),
                command.size(),
                command.price(),
                clock.instant());

        Portfolio portfolio = portfolios.lockForUpdate(command.customerId());
        portfolio.reserve(order.reservation());
        portfolios.save(portfolio);

        Order placed = orders.save(order);
        events.publishEvent(OrderPlaced.of(placed));
        return OrderView.from(placed);
    }
}
