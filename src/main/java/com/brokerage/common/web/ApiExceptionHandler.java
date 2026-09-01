package com.brokerage.common.web;

import com.brokerage.asset.domain.AssetNotHeldException;
import com.brokerage.asset.domain.InsufficientUsableBalanceException;
import com.brokerage.common.domain.ForbiddenException;
import com.brokerage.common.domain.InvariantViolationException;
import com.brokerage.order.domain.IllegalOrderTransitionException;
import com.brokerage.order.domain.InvalidOrderException;
import com.brokerage.order.domain.OrderNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InsufficientUsableBalanceException.class)
    ProblemDetail handleInsufficientBalance(InsufficientUsableBalanceException e) {
        ProblemDetail problem = ProblemDetails.of(
                HttpStatus.UNPROCESSABLE_ENTITY, e.code(), e.getMessage());
        problem.setProperty("assetName", e.assetName().value());
        problem.setProperty("required", e.required().toPlainBigDecimal());
        problem.setProperty("available", e.available().toPlainBigDecimal());
        return problem;
    }

    @ExceptionHandler(AssetNotHeldException.class)
    ProblemDetail handleAssetNotHeld(AssetNotHeldException e) {
        return ProblemDetails.of(HttpStatus.UNPROCESSABLE_ENTITY, e.code(), e.getMessage());
    }

    @ExceptionHandler(InvalidOrderException.class)
    ProblemDetail handleInvalidOrder(InvalidOrderException e) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, e.code(), e.getMessage());
    }

    @ExceptionHandler(IllegalOrderTransitionException.class)
    ProblemDetail handleIllegalTransition(IllegalOrderTransitionException e) {
        return ProblemDetails.of(HttpStatus.CONFLICT, e.code(), e.getMessage());
    }

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail handleNotFound(OrderNotFoundException e) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, e.code(), e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    ProblemDetail handleForbidden(ForbiddenException e) {
        return ProblemDetails.of(HttpStatus.FORBIDDEN, e.code(), e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException e) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage());
    }

    @ExceptionHandler({
            OptimisticLockingFailureException.class,
            PessimisticLockingFailureException.class,
            CannotAcquireLockException.class})
    ProblemDetail handleLockConflict(Exception e) {
        log.warn("Concurrent modification conflict: {}", e.getMessage());
        return ProblemDetails.of(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "The request conflicted with a concurrent operation. Please retry.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("Data integrity violation", e);
        return ProblemDetails.of(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
                "The request violated a data constraint.");
    }

    @ExceptionHandler(InvariantViolationException.class)
    ProblemDetail handleInvariantViolation(InvariantViolationException e) {
        log.error("Domain invariant violated", e);
        return ProblemDetails.of(HttpStatus.INTERNAL_SERVER_ERROR, e.code(),
                "The request could not be completed.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        ProblemDetail problem = ProblemDetails.of(
                HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed");
        List<Map<String, String>> violations = e.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", String.valueOf(error.getDefaultMessage())))
                .toList();
        problem.setProperty("violations", violations);
        return ResponseEntity.badRequest().body(problem);
    }
}
