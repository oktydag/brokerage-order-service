package com.brokerage.matching.application.command;

import com.brokerage.asset.domain.Portfolio;
import com.brokerage.asset.domain.PortfolioRepository;
import com.brokerage.common.application.CommandHandler;
import com.brokerage.common.domain.valueobjects.Settlement;
import com.brokerage.order.application.OrderView;
import com.brokerage.order.domain.Order;
import com.brokerage.order.domain.OrderNotFoundException;
import com.brokerage.order.domain.OrderRepository;
import com.brokerage.order.domain.event.OrderMatched;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class MatchOrderHandler implements CommandHandler<MatchOrderCommand, MatchOrderResult> {

    private final OrderRepository orders;
    private final PortfolioRepository portfolios;
    private final ApplicationEventPublisher events;

    public MatchOrderHandler(OrderRepository orders, PortfolioRepository portfolios,
                             ApplicationEventPublisher events) {
        this.orders = orders;
        this.portfolios = portfolios;
        this.events = events;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MatchOrderResult handle(MatchOrderCommand command) {
        Order order = orders.findByIdForUpdate(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        Portfolio portfolio = portfolios.lockForUpdate(order.getCustomerId());
        Optional<Settlement> settlement = order.match();
        settlement.ifPresent(applied -> {
            portfolio.settle(applied);
            portfolios.save(portfolio);
            events.publishEvent(OrderMatched.of(order));
        });

        return new MatchOrderResult(OrderView.from(order), settlement.isPresent());
    }
}
