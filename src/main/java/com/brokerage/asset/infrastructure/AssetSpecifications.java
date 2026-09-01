package com.brokerage.asset.infrastructure;

import com.brokerage.asset.application.AssetQuery;
import com.brokerage.asset.domain.Asset;
import com.brokerage.common.domain.Amount;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class AssetSpecifications {

    private AssetSpecifications() {
    }

    public static Specification<Asset> matching(AssetQuery query) {
        return (root, criteriaQuery, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("customerId"), query.customerId()));
            if (query.assetName() != null) {
                predicates.add(cb.equal(root.get("assetName"), query.assetName()));
            }
            if (query.nonZeroOnly()) {
                predicates.add(cb.gt(root.get("size").as(java.math.BigDecimal.class),
                        Amount.ZERO.toBigDecimal()));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
