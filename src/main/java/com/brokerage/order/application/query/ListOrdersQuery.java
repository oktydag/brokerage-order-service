package com.brokerage.order.application.query;

import com.brokerage.common.domain.valueobjects.AssetName;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.order.domain.valueobjects.OrderSide;
import com.brokerage.order.domain.valueobjects.OrderStatus;
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
