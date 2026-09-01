package com.brokerage.asset.application;

import com.brokerage.common.domain.AssetName;
import com.brokerage.common.domain.CustomerId;

public record AssetQuery(CustomerId customerId, AssetName assetName, boolean nonZeroOnly) {
}
