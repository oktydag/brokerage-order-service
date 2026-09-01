package com.brokerage.asset.application.query;

import com.brokerage.common.domain.AssetName;
import com.brokerage.common.domain.CustomerId;
import org.springframework.data.domain.Pageable;

public record ListAssetsQuery(
        CustomerId customerId,
        AssetName assetName,
        boolean nonZeroOnly,
        Pageable pageable) {
}
