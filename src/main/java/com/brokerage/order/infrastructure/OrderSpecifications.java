package com.brokerage.order.infrastructure;

import com.brokerage.order.application.OrderQuery;
import com.brokerage.order.domain.Order;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<Order> matching(OrderQuery query) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("customerId"), query.customerId()));

            if (query.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createDate"), query.from()));
            }
            if (query.to() != null) {
                predicates.add(cb.lessThan(root.get("createDate"), query.to()));
            }
            if (query.statuses() != null && !query.statuses().isEmpty()) {
                predicates.add(root.get("status").in(query.statuses()));
            }
            if (query.assetName() != null) {
                predicates.add(cb.equal(root.get("assetName"), query.assetName()));
            }
            if (query.orderSide() != null) {
                predicates.add(cb.equal(root.get("orderSide"), query.orderSide()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
