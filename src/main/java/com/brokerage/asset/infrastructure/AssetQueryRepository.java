package com.brokerage.asset.infrastructure;

import com.brokerage.asset.domain.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface AssetQueryRepository
        extends JpaRepository<Asset, UUID>, JpaSpecificationExecutor<Asset> {
}
