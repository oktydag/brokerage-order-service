package com.brokerage.order.application.query;

import com.brokerage.common.application.QueryHandler;
import com.brokerage.order.application.OrderView;
import com.brokerage.order.domain.Order;
import com.brokerage.order.domain.OrderNotFoundException;
import com.brokerage.order.infrastructure.OrderQueryRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GetOrderHandler implements QueryHandler<GetOrderQuery, OrderView> {

    private final OrderQueryRepository orders;

    public GetOrderHandler(OrderQueryRepository orders) {
        this.orders = orders;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderView handle(GetOrderQuery query) {
        Order order = orders.findById(query.orderId())
                .orElseThrow(() -> new OrderNotFoundException(query.orderId()));
        query.scope().assertCovers(order.getCustomerId());
        return OrderView.from(order);
    }
}
