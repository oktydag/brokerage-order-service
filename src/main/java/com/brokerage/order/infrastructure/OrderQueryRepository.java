package com.brokerage.order.infrastructure;

import com.brokerage.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface OrderQueryRepository
        extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {
}
