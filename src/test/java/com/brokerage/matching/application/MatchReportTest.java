package com.brokerage.matching.application;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MatchReportTest {

    @Test
    void countsEachOutcomeKind() {
        MatchReport report = MatchReport.of(List.of(
                MatchOutcome.of(UUID.randomUUID(), true),
                MatchOutcome.of(UUID.randomUUID(), true),
                MatchOutcome.of(UUID.randomUUID(), false),
                MatchOutcome.rejected(UUID.randomUUID(), "CODE", "message")));

        assertThat(report.requested()).isEqualTo(4);
        assertThat(report.matched()).isEqualTo(2);
        assertThat(report.alreadyMatched()).isEqualTo(1);
        assertThat(report.rejected()).isEqualTo(1);
    }

    @Test
    void describesAnAppliedMatch() {
        UUID id = UUID.randomUUID();
        MatchOutcome outcome = MatchOutcome.of(id, true);

        assertThat(outcome.orderId()).isEqualTo(id);
        assertThat(outcome.result()).isEqualTo(MatchOutcome.Result.MATCHED);
        assertThat(outcome.code()).isNull();
        assertThat(outcome.isRejected()).isFalse();
    }

    @Test
    void describesARejection() {
        MatchOutcome outcome = MatchOutcome.rejected(UUID.randomUUID(), "CODE", "message");

        assertThat(outcome.isRejected()).isTrue();
        assertThat(outcome.code()).isEqualTo("CODE");
        assertThat(outcome.message()).isEqualTo("message");
    }
}
