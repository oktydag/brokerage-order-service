package com.brokerage.order.application;

import com.brokerage.common.domain.Amount;
import com.brokerage.common.domain.AssetName;
import com.brokerage.common.domain.CustomerId;
import com.brokerage.order.domain.OrderSide;

public record PlaceOrderCommand(
        CustomerId customerId,
        AssetName assetName,
        OrderSide orderSide,
        Amount size,
        Amount price) {
}
