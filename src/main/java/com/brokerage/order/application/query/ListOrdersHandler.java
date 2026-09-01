package com.brokerage.order.application.query;

import com.brokerage.common.application.QueryHandler;
import com.brokerage.common.web.PageResponse;
import com.brokerage.order.application.OrderView;
import com.brokerage.order.infrastructure.OrderQueryRepository;
import com.brokerage.order.infrastructure.OrderSpecifications;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListOrdersHandler implements QueryHandler<ListOrdersQuery, PageResponse<OrderView>> {

    private final OrderQueryRepository orders;

    public ListOrdersHandler(OrderQueryRepository orders) {
        this.orders = orders;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderView> handle(ListOrdersQuery query) {
        return PageResponse.from(
                orders.findAll(OrderSpecifications.matching(query), query.pageable()),
                OrderView::from);
    }
}
