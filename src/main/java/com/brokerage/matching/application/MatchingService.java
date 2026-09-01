package com.brokerage.matching.application;

import com.brokerage.common.domain.DomainException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Service
public class MatchingService {

    public static final int MAX_BATCH_SIZE = 500;

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    private final OrderMatcher matcher;

    public MatchingService(OrderMatcher matcher) {
        this.matcher = matcher;
    }

    public MatchReport match(List<UUID> orderIds) {
        List<UUID> distinct = new ArrayList<>(new LinkedHashSet<>(orderIds));
        if (distinct.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException(
                    "at most %d orders may be matched per request".formatted(MAX_BATCH_SIZE));
        }

        List<MatchOutcome> outcomes = new ArrayList<>(distinct.size());
        for (UUID orderId : distinct) {
            outcomes.add(matchOne(orderId));
        }
        return MatchReport.of(outcomes);
    }

    private MatchOutcome matchOne(UUID orderId) {
        try {
            matcher.match(orderId);
            return MatchOutcome.matched(orderId);
        } catch (DomainException e) {
            return MatchOutcome.rejected(orderId, e.code(), e.getMessage());
        } catch (RuntimeException e) {
            log.error("Unexpected failure while matching order {}", orderId, e);
            return MatchOutcome.rejected(orderId, "MATCH_FAILED", "Order could not be matched");
        }
    }
}
