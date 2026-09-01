package com.brokerage.order.application;

import com.brokerage.asset.domain.Portfolio;
import com.brokerage.asset.domain.PortfolioRepository;
import com.brokerage.common.domain.AccessScope;
import com.brokerage.order.domain.Order;
import com.brokerage.order.domain.OrderNotFoundException;
import com.brokerage.order.domain.OrderRepository;
import com.brokerage.order.domain.Reservation;
import com.brokerage.order.domain.event.OrderCanceled;
import com.brokerage.order.domain.event.OrderPlaced;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class OrderCommandService {

    private final OrderRepository orders;
    private final PortfolioRepository portfolios;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public OrderCommandService(OrderRepository orders, PortfolioRepository portfolios,
                               ApplicationEventPublisher events, Clock clock) {
        this.orders = orders;
        this.portfolios = portfolios;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public OrderView place(PlaceOrderCommand command) {
        Order order = Order.place(
                command.customerId(),
                command.assetName(),
                command.orderSide(),
                command.size(),
                command.price(),
                clock.instant());

        Portfolio portfolio = portfolios.lockForUpdate(command.customerId());
        Reservation reservation = order.reservation();
        portfolio.reserve(reservation.assetName(), reservation.amount());
        portfolios.save(portfolio);

        Order placed = orders.save(order);
        events.publishEvent(OrderPlaced.of(placed));
        return OrderView.from(placed);
    }

    @Transactional
    public OrderView cancel(UUID orderId, AccessScope scope) {
        Order order = orders.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        scope.assertCovers(order.getCustomerId());

        Portfolio portfolio = portfolios.lockForUpdate(order.getCustomerId());
        order.cancel();

        Reservation reservation = order.reservation();
        portfolio.release(reservation.assetName(), reservation.amount());

        events.publishEvent(OrderCanceled.of(order));
        return OrderView.from(order);
    }
}
