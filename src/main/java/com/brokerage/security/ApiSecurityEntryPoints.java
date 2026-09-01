package com.brokerage.security;

import com.brokerage.common.web.ProblemDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ApiSecurityEntryPoints {

    private static final String REALM = "brokerage";

    private final ObjectMapper objectMapper;

    public ApiSecurityEntryPoints(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"%s\"".formatted(REALM));
            write(response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED",
                    "Valid credentials are required.");
        };
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                write(response, HttpStatus.FORBIDDEN, "FORBIDDEN",
                        "The authenticated principal may not perform this operation.");
    }

    private void write(HttpServletResponse response, HttpStatus status, String code, String detail)
            throws IOException {
        ProblemDetail problem = ProblemDetails.of(status, code, detail);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
