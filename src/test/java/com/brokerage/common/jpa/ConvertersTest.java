package com.brokerage.common.jpa;

import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.common.domain.valueobjects.AssetName;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.common.domain.valueobjects.IdempotencyKey;
import com.brokerage.common.domain.valueobjects.RequestFingerprint;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ConvertersTest {

    @Test
    void amountRoundTripsThroughTheColumn() {
        AmountConverter converter = new AmountConverter();

        assertThat(converter.convertToDatabaseColumn(Amount.of("12.5")))
                .isEqualByComparingTo("12.5");
        assertThat(converter.convertToEntityAttribute(new BigDecimal("12.5")))
                .isEqualTo(Amount.of("12.5"));
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void customerIdRoundTripsThroughTheColumn() {
        CustomerIdConverter converter = new CustomerIdConverter();

        assertThat(converter.convertToDatabaseColumn(CustomerId.of("CUST-1"))).isEqualTo("CUST-1");
        assertThat(converter.convertToEntityAttribute("CUST-1")).isEqualTo(CustomerId.of("CUST-1"));
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void assetNameRoundTripsThroughTheColumn() {
        AssetNameConverter converter = new AssetNameConverter();

        assertThat(converter.convertToDatabaseColumn(AssetName.of("thyao"))).isEqualTo("THYAO");
        assertThat(converter.convertToEntityAttribute("THYAO")).isEqualTo(AssetName.of("THYAO"));
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void idempotencyKeyRoundTripsThroughTheColumn() {
        IdempotencyKeyConverter converter = new IdempotencyKeyConverter();

        assertThat(converter.convertToDatabaseColumn(IdempotencyKey.of("K1"))).isEqualTo("K1");
        assertThat(converter.convertToEntityAttribute("K1")).isEqualTo(IdempotencyKey.of("K1"));
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void fingerprintRoundTripsThroughTheColumn() {
        RequestFingerprintConverter converter = new RequestFingerprintConverter();
        RequestFingerprint fingerprint = RequestFingerprint.over("a");

        assertThat(converter.convertToDatabaseColumn(fingerprint)).isEqualTo(fingerprint.value());
        assertThat(converter.convertToEntityAttribute(fingerprint.value())).isEqualTo(fingerprint);
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
