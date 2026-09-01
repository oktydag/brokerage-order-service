package com.brokerage.common.domain.valueobjects;

import java.io.Serializable;
import java.util.Locale;

public record AssetName(String value) implements Serializable {

    public static final AssetName TRY = new AssetName("TRY");

    public AssetName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("assetName must not be blank");
        }
        value = value.trim().toUpperCase(Locale.ROOT);
    }

    public static AssetName of(String value) {
        return new AssetName(value);
    }

    public boolean isCurrency() {
        return TRY.equals(this);
    }

    @Override
    public String toString() {
        return value;
    }
}
