package com.brokerage.order.application.command;

import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.common.domain.valueobjects.AssetName;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.order.domain.valueobjects.OrderSide;

public record PlaceOrderCommand(
        CustomerId customerId,
        AssetName assetName,
        OrderSide orderSide,
        Amount size,
        Amount price) {
}
