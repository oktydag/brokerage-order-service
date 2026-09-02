package com.brokerage.support;

import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.common.domain.valueobjects.AssetName;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.order.domain.Order;
import com.brokerage.order.domain.valueobjects.OrderSide;

import java.time.Instant;

public final class Fixtures {

    public static final CustomerId CUSTOMER = CustomerId.of("CUST-1");
    public static final AssetName THYAO = AssetName.of("THYAO");
    public static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    private Fixtures() {
    }

    public static Order buyOrder() {
        return Order.place(CUSTOMER, THYAO, OrderSide.BUY, Amount.of(100), Amount.of(300), NOW);
    }

    public static Order sellOrder() {
        return Order.place(CUSTOMER, THYAO, OrderSide.SELL, Amount.of(50), Amount.of(200), NOW);
    }
}
