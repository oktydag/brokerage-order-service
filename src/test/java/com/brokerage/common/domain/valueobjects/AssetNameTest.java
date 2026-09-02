package com.brokerage.common.domain.valueobjects;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssetNameTest {

    @Test
    void normalisesToUpperCase() {
        assertThat(AssetName.of(" thyao ")).isEqualTo(AssetName.of("THYAO"));
        assertThat(AssetName.of("thyao").value()).isEqualTo("THYAO");
    }

    @Test
    void rejectsBlankNames() {
        assertThatThrownBy(() -> AssetName.of("  ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AssetName.of(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recognisesTheSettlementCurrency() {
        assertThat(AssetName.TRY.isCurrency()).isTrue();
        assertThat(AssetName.of("try").isCurrency()).isTrue();
        assertThat(AssetName.of("THYAO").isCurrency()).isFalse();
    }

    @Test
    void printsItsValue() {
        assertThat(AssetName.of("GARAN")).hasToString("GARAN");
    }
}
