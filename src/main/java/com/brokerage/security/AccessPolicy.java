package com.brokerage.security;

import com.brokerage.common.domain.AccessScope;
import com.brokerage.common.domain.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AccessPolicy {

    public AccessScope currentScope() {
        AuthenticatedUser principal = currentPrincipal();
        if (principal.role() == Role.ADMIN) {
            return AccessScope.unrestricted();
        }
        return principal.customerId()
                .map(AccessScope::of)
                .orElseThrow(() -> new ForbiddenException(
                        "Credential is not linked to a customer account"));
    }

    private AuthenticatedUser currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new ForbiddenException("Unauthenticated request");
        }
        return user;
    }
}
