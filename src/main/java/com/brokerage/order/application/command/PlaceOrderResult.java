package com.brokerage.order.application.command;

import com.brokerage.order.application.OrderView;

public record PlaceOrderResult(OrderView order, boolean replayed) {
}
