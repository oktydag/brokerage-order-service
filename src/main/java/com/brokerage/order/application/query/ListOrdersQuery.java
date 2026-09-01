package com.brokerage.order.application.query;

import com.brokerage.common.domain.AssetName;
import com.brokerage.common.domain.CustomerId;
import com.brokerage.order.domain.OrderSide;
import com.brokerage.order.domain.OrderStatus;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Set;

public record ListOrdersQuery(
        CustomerId customerId,
        Instant from,
        Instant to,
        Set<OrderStatus> statuses,
        AssetName assetName,
        OrderSide orderSide,
        Pageable pageable) {
}
