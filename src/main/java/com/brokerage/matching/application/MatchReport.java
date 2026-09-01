package com.brokerage.matching.application;

import java.util.List;

public record MatchReport(int requested, int matched, int alreadyMatched, int rejected,
                          List<MatchOutcome> outcomes) {

    public static MatchReport of(List<MatchOutcome> outcomes) {
        return new MatchReport(
                outcomes.size(),
                count(outcomes, MatchOutcome.Result.MATCHED),
                count(outcomes, MatchOutcome.Result.ALREADY_MATCHED),
                count(outcomes, MatchOutcome.Result.REJECTED),
                outcomes);
    }

    private static int count(List<MatchOutcome> outcomes, MatchOutcome.Result result) {
        return (int) outcomes.stream().filter(outcome -> outcome.result() == result).count();
    }
}
