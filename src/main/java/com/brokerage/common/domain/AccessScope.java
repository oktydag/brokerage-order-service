package com.brokerage.common.domain;

import java.util.Optional;

public final class AccessScope {

    private static final AccessScope UNRESTRICTED = new AccessScope(null);

    private final CustomerId customerId;

    private AccessScope(CustomerId customerId) {
        this.customerId = customerId;
    }

    public static AccessScope unrestricted() {
        return UNRESTRICTED;
    }

    public static AccessScope of(CustomerId customerId) {
        return new AccessScope(customerId);
    }

    public boolean isUnrestricted() {
        return customerId == null;
    }

    public Optional<CustomerId> customerId() {
        return Optional.ofNullable(customerId);
    }

    public CustomerId resolveTarget(CustomerId requested) {
        if (isUnrestricted()) {
            if (requested == null) {
                throw new IllegalArgumentException("customerId is required");
            }
            return requested;
        }
        if (requested != null && !requested.equals(customerId)) {
            throw new ForbiddenException("Access to customer %s is not permitted".formatted(requested));
        }
        return customerId;
    }

    public void assertCovers(CustomerId owner) {
        if (!isUnrestricted() && !owner.equals(customerId)) {
            throw new ForbiddenException("Access to customer %s is not permitted".formatted(owner));
        }
    }
}
