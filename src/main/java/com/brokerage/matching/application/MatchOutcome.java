package com.brokerage.matching.application;

import java.util.UUID;

public record MatchOutcome(UUID orderId, Result result, String code, String message) {

    public enum Result {
        MATCHED,
        ALREADY_MATCHED,
        REJECTED
    }

    public static MatchOutcome of(UUID orderId, boolean applied) {
        return applied
                ? new MatchOutcome(orderId, Result.MATCHED, null, null)
                : new MatchOutcome(orderId, Result.ALREADY_MATCHED, null, null);
    }

    public static MatchOutcome rejected(UUID orderId, String code, String message) {
        return new MatchOutcome(orderId, Result.REJECTED, code, message);
    }

    public boolean isRejected() {
        return result == Result.REJECTED;
    }
}
