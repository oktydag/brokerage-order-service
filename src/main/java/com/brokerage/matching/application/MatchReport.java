package com.brokerage.matching.application;

import java.util.List;

public record MatchReport(int requested, int matched, int rejected, List<MatchOutcome> outcomes) {

    public static MatchReport of(List<MatchOutcome> outcomes) {
        int matched = (int) outcomes.stream()
                .filter(o -> o.result() == MatchOutcome.Result.MATCHED)
                .count();
        return new MatchReport(outcomes.size(), matched, outcomes.size() - matched, outcomes);
    }
}
