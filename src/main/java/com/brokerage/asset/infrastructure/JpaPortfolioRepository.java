package com.brokerage.asset.infrastructure;

import com.brokerage.asset.domain.Asset;
import com.brokerage.asset.domain.Portfolio;
import com.brokerage.asset.domain.PortfolioRepository;
import com.brokerage.common.domain.CustomerId;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JpaPortfolioRepository implements PortfolioRepository {

    private final AssetJpaRepository assets;

    public JpaPortfolioRepository(AssetJpaRepository assets) {
        this.assets = assets;
    }

    @Override
    public Portfolio lockForUpdate(CustomerId customerId) {
        return Portfolio.of(customerId, assets.lockByCustomerId(customerId));
    }

    @Override
    public Portfolio load(CustomerId customerId) {
        return Portfolio.of(customerId, assets.findByCustomerId(customerId));
    }

    @Override
    public void save(Portfolio portfolio) {
        List<Asset> opened = portfolio.newlyCreated();
        if (!opened.isEmpty()) {
            assets.saveAll(opened);
        }
    }
}
