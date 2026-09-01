package com.brokerage.order.application;

import com.brokerage.common.domain.AccessScope;
import com.brokerage.common.web.PageResponse;
import com.brokerage.order.domain.Order;
import com.brokerage.order.domain.OrderNotFoundException;
import com.brokerage.order.infrastructure.OrderQueryRepository;
import com.brokerage.order.infrastructure.OrderSpecifications;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OrderQueryService {

    private final OrderQueryRepository orders;

    public OrderQueryService(OrderQueryRepository orders) {
        this.orders = orders;
    }

    public PageResponse<OrderView> list(OrderQuery query, Pageable pageable) {
        return PageResponse.from(
                orders.findAll(OrderSpecifications.matching(query), pageable),
                OrderView::from);
    }

    public OrderView get(UUID orderId, AccessScope scope) {
        Order order = orders.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        scope.assertCovers(order.getCustomerId());
        return OrderView.from(order);
    }
}
