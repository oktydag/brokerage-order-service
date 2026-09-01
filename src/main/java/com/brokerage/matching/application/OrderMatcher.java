package com.brokerage.matching.application;

import com.brokerage.asset.domain.Portfolio;
import com.brokerage.asset.domain.PortfolioRepository;
import com.brokerage.order.domain.Order;
import com.brokerage.order.domain.OrderNotFoundException;
import com.brokerage.order.domain.OrderRepository;
import com.brokerage.order.domain.Settlement;
import com.brokerage.order.domain.event.OrderMatched;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class OrderMatcher {

    private final OrderRepository orders;
    private final PortfolioRepository portfolios;
    private final ApplicationEventPublisher events;

    public OrderMatcher(OrderRepository orders, PortfolioRepository portfolios,
                        ApplicationEventPublisher events) {
        this.orders = orders;
        this.portfolios = portfolios;
        this.events = events;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void match(UUID orderId) {
        Order order = orders.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        Portfolio portfolio = portfolios.lockForUpdate(order.getCustomerId());
        order.match();

        Settlement settlement = order.settlement();
        portfolio.settleOutgoing(settlement.outgoing().assetName(), settlement.outgoing().amount());
        portfolio.settleIncoming(settlement.incoming().assetName(), settlement.incoming().amount());
        portfolios.save(portfolio);

        events.publishEvent(OrderMatched.of(order));
    }
}
