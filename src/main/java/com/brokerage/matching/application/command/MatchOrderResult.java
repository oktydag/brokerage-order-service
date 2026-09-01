package com.brokerage.matching.application.command;

import com.brokerage.order.application.OrderView;

public record MatchOrderResult(OrderView order, boolean applied) {
}
