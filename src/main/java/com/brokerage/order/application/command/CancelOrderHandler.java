package com.brokerage.order.application.command;

import com.brokerage.asset.domain.Portfolio;
import com.brokerage.asset.domain.PortfolioRepository;
import com.brokerage.common.application.CommandHandler;
import com.brokerage.order.application.OrderView;
import com.brokerage.order.domain.Order;
import com.brokerage.order.domain.OrderNotFoundException;
import com.brokerage.order.domain.OrderRepository;
import com.brokerage.order.domain.event.OrderCanceled;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CancelOrderHandler implements CommandHandler<CancelOrderCommand, OrderView> {

    private final OrderRepository orders;
    private final PortfolioRepository portfolios;
    private final ApplicationEventPublisher events;

    public CancelOrderHandler(OrderRepository orders, PortfolioRepository portfolios,
                              ApplicationEventPublisher events) {
        this.orders = orders;
        this.portfolios = portfolios;
        this.events = events;
    }

    @Override
    @Transactional
    public OrderView handle(CancelOrderCommand command) {
        Order order = orders.findByIdForUpdate(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));
        command.scope().assertCovers(order.getCustomerId());

        Portfolio portfolio = portfolios.lockForUpdate(order.getCustomerId());
        order.cancel();
        portfolio.release(order.reservation());

        events.publishEvent(OrderCanceled.of(order));
        return OrderView.from(order);
    }
}
