package com.brokerage.matching.application.command;

import com.brokerage.common.application.CommandHandler;
import com.brokerage.common.domain.DomainException;
import com.brokerage.matching.application.MatchOutcome;
import com.brokerage.matching.application.MatchReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Component
public class MatchOrdersHandler implements CommandHandler<MatchOrdersCommand, MatchReport> {

    public static final int MAX_BATCH_SIZE = 500;

    private static final Logger log = LoggerFactory.getLogger(MatchOrdersHandler.class);

    private final MatchOrderHandler matchOrder;

    public MatchOrdersHandler(MatchOrderHandler matchOrder) {
        this.matchOrder = matchOrder;
    }

    @Override
    public MatchReport handle(MatchOrdersCommand command) {
        List<UUID> distinct = new ArrayList<>(new LinkedHashSet<>(command.orderIds()));
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
            matchOrder.handle(new MatchOrderCommand(orderId));
            return MatchOutcome.matched(orderId);
        } catch (DomainException e) {
            return MatchOutcome.rejected(orderId, e.code(), e.getMessage());
        } catch (RuntimeException e) {
            log.error("Unexpected failure while matching order {}", orderId, e);
            return MatchOutcome.rejected(orderId, "MATCH_FAILED", "Order could not be matched");
        }
    }
}
