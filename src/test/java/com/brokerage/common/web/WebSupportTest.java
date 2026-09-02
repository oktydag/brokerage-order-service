package com.brokerage.common.web;

import com.brokerage.asset.domain.AssetNotHeldException;
import com.brokerage.asset.domain.InsufficientUsableBalanceException;
import com.brokerage.common.domain.ForbiddenException;
import com.brokerage.common.domain.InvariantViolationException;
import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.common.domain.valueobjects.AssetName;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.common.domain.valueobjects.IdempotencyKey;
import com.brokerage.common.idempotency.DuplicateRequestException;
import com.brokerage.common.idempotency.IdempotencyKeyReuseException;
import com.brokerage.order.domain.IllegalOrderTransitionException;
import com.brokerage.order.domain.InvalidOrderException;
import com.brokerage.order.domain.OrderNotFoundException;
import com.brokerage.order.domain.valueobjects.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WebSupportTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void pageResponseMirrorsTheUnderlyingPage() {
        PageResponse<String> page = PageResponse.from(
                new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 6), String::toUpperCase);

        assertThat(page.content()).containsExactly("A", "B");
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(6);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.last()).isFalse();
    }

    @Test
    void problemDetailsCarryTheMachineReadableCode() {
        ProblemDetail problem = ProblemDetails.of(HttpStatus.CONFLICT, "SOME_CODE", "detail");

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getTitle()).isEqualTo("Conflict");
        assertThat(problem.getDetail()).isEqualTo("detail");
        assertThat(problem.getType().toString()).endsWith("/some_code");
        assertThat(problem.getProperties()).containsKey("timestamp");
        assertThat(problem.getProperties()).containsEntry("code", "SOME_CODE");
    }

    @Test
    void insufficientBalanceIsUnprocessableAndExposesTheShortfall() {
        ProblemDetail problem = handler.handleInsufficientBalance(new InsufficientUsableBalanceException(
                CustomerId.of("CUST-1"), AssetName.TRY, Amount.of(100), Amount.of(40)));

        assertThat(problem.getStatus()).isEqualTo(422);
        assertThat(problem.getProperties()).containsEntry("assetName", "TRY");
        assertThat(problem.getProperties()).containsEntry("required", Amount.of(100).toPlainBigDecimal());
        assertThat(problem.getProperties()).containsEntry("available", Amount.of(40).toPlainBigDecimal());
    }

    @Test
    void mapsEachDomainFailureToItsTransportStatus() {
        assertThat(handler.handleAssetNotHeld(
                new AssetNotHeldException(CustomerId.of("C"), AssetName.TRY)).getStatus()).isEqualTo(422);
        assertThat(handler.handleIdempotencyKeyReuse(
                new IdempotencyKeyReuseException(IdempotencyKey.of("K"))).getStatus()).isEqualTo(422);
        assertThat(handler.handleInvalidOrder(new InvalidOrderException("bad")).getStatus()).isEqualTo(400);
        assertThat(handler.handleIllegalArgument(new IllegalArgumentException("bad")).getStatus()).isEqualTo(400);
        assertThat(handler.handleIllegalTransition(new IllegalOrderTransitionException(
                UUID.randomUUID(), OrderStatus.MATCHED, OrderStatus.CANCELED)).getStatus()).isEqualTo(409);
        assertThat(handler.handleDuplicateRequest(
                new DuplicateRequestException(IdempotencyKey.of("K"))).getStatus()).isEqualTo(409);
        assertThat(handler.handleNotFound(
                new OrderNotFoundException(UUID.randomUUID())).getStatus()).isEqualTo(404);
        assertThat(handler.handleForbidden(new ForbiddenException("no")).getStatus()).isEqualTo(403);
    }

    @Test
    void reportsConcurrencyConflictsAsRetryable() {
        assertThat(handler.handleLockConflict(new OptimisticLockingFailureException("v"))
                .getProperties()).containsEntry("code", "CONCURRENT_MODIFICATION");
        assertThat(handler.handleLockConflict(new CannotAcquireLockException("l")).getStatus()).isEqualTo(409);
        assertThat(handler.handleDataIntegrity(new DataIntegrityViolationException("c"))
                .getStatus()).isEqualTo(409);
    }

    @Test
    void reportsAnInvariantBreachAsAServerFaultWithoutLeakingInternals() {
        ProblemDetail problem = handler.handleInvariantViolation(
                new InvariantViolationException("usableSize exceeds size"));

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getDetail()).doesNotContain("usableSize");
    }
}
