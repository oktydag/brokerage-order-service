package com.brokerage.matching.application;

import java.util.UUID;

public record MatchOutcome(UUID orderId, Result result, String code, String message) {

    public enum Result {
        MATCHED,
        REJECTED
    }

    public static MatchOutcome matched(UUID orderId) {
        return new MatchOutcome(orderId, Result.MATCHED, null, null);
    }

    public static MatchOutcome rejected(UUID orderId, String code, String message) {
        return new MatchOutcome(orderId, Result.REJECTED, code, message);
    }
}
