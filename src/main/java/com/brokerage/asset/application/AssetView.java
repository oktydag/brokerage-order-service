package com.brokerage.asset.application;

import com.brokerage.asset.domain.Asset;

import java.math.BigDecimal;

public record AssetView(
        String customerId,
        String assetName,
        BigDecimal size,
        BigDecimal usableSize,
        BigDecimal reservedSize) {

    public static AssetView from(Asset asset) {
        return new AssetView(
                asset.getCustomerId().value(),
                asset.getAssetName().value(),
                asset.getSize().toPlainBigDecimal(),
                asset.getUsableSize().toPlainBigDecimal(),
                asset.getReservedSize().toPlainBigDecimal());
    }
}
