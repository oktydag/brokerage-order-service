package com.brokerage.asset.application;

import com.brokerage.asset.infrastructure.AssetQueryRepository;
import com.brokerage.asset.infrastructure.AssetSpecifications;
import com.brokerage.common.web.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AssetQueryService {

    private final AssetQueryRepository assets;

    public AssetQueryService(AssetQueryRepository assets) {
        this.assets = assets;
    }

    public PageResponse<AssetView> list(AssetQuery query, Pageable pageable) {
        return PageResponse.from(
                assets.findAll(AssetSpecifications.matching(query), pageable),
                AssetView::from);
    }
}
