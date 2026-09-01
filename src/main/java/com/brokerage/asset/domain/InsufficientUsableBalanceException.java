package com.brokerage.asset.domain;

import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.common.domain.valueobjects.AssetName;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.common.domain.DomainException;

public class InsufficientUsableBalanceException extends DomainException {

    private final transient CustomerId customerId;
    private final transient AssetName assetName;
    private final transient Amount required;
    private final transient Amount available;

    public InsufficientUsableBalanceException(
            CustomerId customerId, AssetName assetName, Amount required, Amount available) {
        super("INSUFFICIENT_USABLE_BALANCE",
                "Customer %s has %s usable %s but %s is required"
                        .formatted(customerId, available, assetName, required));
        this.customerId = customerId;
        this.assetName = assetName;
        this.required = required;
        this.available = available;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public AssetName assetName() {
        return assetName;
    }

    public Amount required() {
        return required;
    }

    public Amount available() {
        return available;
    }
}
