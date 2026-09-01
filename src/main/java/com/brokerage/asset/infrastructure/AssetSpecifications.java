package com.brokerage.asset.infrastructure;

import com.brokerage.asset.application.query.ListAssetsQuery;
import com.brokerage.asset.domain.Asset;
import com.brokerage.common.domain.Amount;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class AssetSpecifications {

    private AssetSpecifications() {
    }

    public static Specification<Asset> matching(ListAssetsQuery query) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("customerId"), query.customerId()));
            if (query.assetName() != null) {
                predicates.add(cb.equal(root.get("assetName"), query.assetName()));
            }
            if (query.nonZeroOnly()) {
                predicates.add(cb.gt(root.get("size").as(BigDecimal.class),
                        Amount.ZERO.toBigDecimal()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
