package com.brokerage.asset.domain;

import com.brokerage.common.domain.AssetName;
import com.brokerage.common.domain.CustomerId;
import com.brokerage.common.domain.DomainException;

public class AssetNotHeldException extends DomainException {

    public AssetNotHeldException(CustomerId customerId, AssetName assetName) {
        super("ASSET_NOT_HELD", "Customer %s holds no %s".formatted(customerId, assetName));
    }
}
