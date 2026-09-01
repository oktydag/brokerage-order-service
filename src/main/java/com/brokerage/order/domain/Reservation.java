package com.brokerage.order.domain;

import com.brokerage.common.domain.Amount;
import com.brokerage.common.domain.AssetName;

public record Reservation(AssetName assetName, Amount amount) {
}
