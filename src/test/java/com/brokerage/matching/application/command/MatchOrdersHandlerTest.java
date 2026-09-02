package com.brokerage.matching.application.command;

import com.brokerage.matching.application.MatchOutcome;
import com.brokerage.matching.application.MatchReport;
import com.brokerage.order.application.OrderView;
import com.brokerage.order.domain.IllegalOrderTransitionException;
import com.brokerage.order.domain.valueobjects.OrderStatus;
import com.brokerage.support.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchOrdersHandlerTest {

    @Mock
    private MatchOrderHandler matchOrder;

    private MatchOrdersHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MatchOrdersHandler(matchOrder);
    }

    private MatchOrderResult result(boolean applied) {
        return new MatchOrderResult(OrderView.from(Fixtures.buyOrder()), applied);
    }

    @Test
    void reportsEachOrderSeparately() {
        UUID matched = UUID.randomUUID();
        UUID already = UUID.randomUUID();
        UUID rejected = UUID.randomUUID();
        when(matchOrder.handle(new MatchOrderCommand(matched))).thenReturn(result(true));
        when(matchOrder.handle(new MatchOrderCommand(already))).thenReturn(result(false));
        when(matchOrder.handle(new MatchOrderCommand(rejected)))
                .thenThrow(new IllegalOrderTransitionException(rejected, OrderStatus.CANCELED, OrderStatus.MATCHED));

        MatchReport report = handler.handle(new MatchOrdersCommand(List.of(matched, already, rejected)));

        assertThat(report.requested()).isEqualTo(3);
        assertThat(report.matched()).isEqualTo(1);
        assertThat(report.alreadyMatched()).isEqualTo(1);
        assertThat(report.rejected()).isEqualTo(1);
        assertThat(report.outcomes()).extracting(MatchOutcome::result).containsExactly(
                MatchOutcome.Result.MATCHED,
                MatchOutcome.Result.ALREADY_MATCHED,
                MatchOutcome.Result.REJECTED);
        assertThat(report.outcomes().get(2).code()).isEqualTo("ILLEGAL_ORDER_TRANSITION");
    }

    @Test
    void oneFailureDoesNotAbortTheBatch() {
        UUID broken = UUID.randomUUID();
        UUID healthy = UUID.randomUUID();
        when(matchOrder.handle(new MatchOrderCommand(broken)))
                .thenThrow(new IllegalStateException("database is on fire"));
        when(matchOrder.handle(new MatchOrderCommand(healthy))).thenReturn(result(true));

        MatchReport report = handler.handle(new MatchOrdersCommand(List.of(broken, healthy)));

        assertThat(report.matched()).isEqualTo(1);
        assertThat(report.rejected()).isEqualTo(1);
        assertThat(report.outcomes().get(0).code()).isEqualTo("MATCH_FAILED");
        assertThat(report.outcomes().get(0).message()).doesNotContain("fire");
    }

    @Test
    void collapsesDuplicateIdsWithinOneBatch() {
        UUID id = UUID.randomUUID();
        when(matchOrder.handle(new MatchOrderCommand(id))).thenReturn(result(true));

        MatchReport report = handler.handle(new MatchOrdersCommand(List.of(id, id, id)));

        assertThat(report.requested()).isEqualTo(1);
        verify(matchOrder, times(1)).handle(new MatchOrderCommand(id));
    }

    @Test
    void boundsTheAmountOfWorkOneRequestCanTrigger() {
        List<UUID> tooMany = IntStream.rangeClosed(0, MatchOrdersHandler.MAX_BATCH_SIZE)
                .mapToObj(index -> UUID.randomUUID())
                .toList();

        assertThatThrownBy(() -> handler.handle(new MatchOrdersCommand(tooMany)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(MatchOrdersHandler.MAX_BATCH_SIZE));
    }

    @Test
    void handlesAnEmptyBatch() {
        MatchReport report = handler.handle(new MatchOrdersCommand(Collections.emptyList()));

        assertThat(report.requested()).isZero();
        assertThat(report.outcomes()).isEmpty();
    }
}
