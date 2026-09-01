package com.brokerage.asset.domain;

import com.brokerage.common.domain.CustomerId;

public interface PortfolioRepository {

    Portfolio lockForUpdate(CustomerId customerId);

    Portfolio load(CustomerId customerId);

    void save(Portfolio portfolio);
}
