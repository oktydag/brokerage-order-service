package com.brokerage.common.domain.valueobjects;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestFingerprintTest {

    @Test
    void isDeterministic() {
        assertThat(RequestFingerprint.over("a", "b")).isEqualTo(RequestFingerprint.over("a", "b"));
    }

    @Test
    void differsWhenAnyPartDiffers() {
        assertThat(RequestFingerprint.over("a", "b")).isNotEqualTo(RequestFingerprint.over("a", "c"));
    }

    @Test
    void cannotBeForgedByMovingTheSeparatorBetweenParts() {
        assertThat(RequestFingerprint.over("a", "b|c")).isNotEqualTo(RequestFingerprint.over("a|b", "c"));
    }

    @Test
    void producesHexOfTheDigestLength() {
        assertThat(RequestFingerprint.over("a").value()).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    void rejectsBlankValues() {
        assertThatThrownBy(() -> new RequestFingerprint(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RequestFingerprint(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void printsItsValue() {
        RequestFingerprint fingerprint = RequestFingerprint.over("a");
        assertThat(fingerprint).hasToString(fingerprint.value());
    }
}
