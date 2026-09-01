package com.brokerage.asset.infrastructure;

import com.brokerage.asset.domain.Asset;
import com.brokerage.common.domain.valueobjects.CustomerId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AssetJpaRepository extends JpaRepository<Asset, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Asset a where a.customerId = :customerId order by a.assetName")
    List<Asset> lockByCustomerId(@Param("customerId") CustomerId customerId);

    @Query("select a from Asset a where a.customerId = :customerId order by a.assetName")
    List<Asset> findByCustomerId(@Param("customerId") CustomerId customerId);
}
