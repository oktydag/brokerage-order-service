package com.brokerage.common.idempotency;

import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.common.domain.valueobjects.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

    Optional<IdempotencyRecord> findByCustomerIdAndIdempotencyKey(
            CustomerId customerId, IdempotencyKey idempotencyKey);

    @Modifying
    @Query("delete from IdempotencyRecord r where r.createdAt < :before")
    int deleteOlderThan(@Param("before") Instant before);
}
