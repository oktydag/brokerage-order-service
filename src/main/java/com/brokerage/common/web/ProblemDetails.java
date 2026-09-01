package com.brokerage.common.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.Instant;

public final class ProblemDetails {

    private ProblemDetails() {
    }

    public static ProblemDetail of(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create("https://api.brokerage.com/problems/" + code.toLowerCase()));
        problem.setProperty("code", code);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
