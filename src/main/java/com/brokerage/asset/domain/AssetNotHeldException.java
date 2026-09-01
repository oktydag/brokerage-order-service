package com.brokerage.asset.domain;

import com.brokerage.common.domain.valueobjects.AssetName;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.common.domain.DomainException;

public class AssetNotHeldException extends DomainException {

    public AssetNotHeldException(CustomerId customerId, AssetName assetName) {
        super("ASSET_NOT_HELD", "Customer %s holds no %s".formatted(customerId, assetName));
    }
}
