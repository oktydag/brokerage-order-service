package com.brokerage.order.application.command;

import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.common.domain.valueobjects.AssetName;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.common.domain.valueobjects.IdempotencyKey;
import com.brokerage.common.domain.valueobjects.RequestFingerprint;
import com.brokerage.order.domain.valueobjects.OrderSide;

public record PlaceOrderCommand(
        CustomerId customerId,
        AssetName assetName,
        OrderSide orderSide,
        Amount size,
        Amount price,
        IdempotencyKey idempotencyKey) {

    public RequestFingerprint fingerprint() {
        return RequestFingerprint.over(
                customerId.value(),
                assetName.value(),
                orderSide.name(),
                size.toBigDecimal().toPlainString(),
                price.toBigDecimal().toPlainString());
    }
}
