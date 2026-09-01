package com.brokerage.asset.application.query;

import com.brokerage.asset.application.AssetView;
import com.brokerage.asset.infrastructure.AssetQueryRepository;
import com.brokerage.asset.infrastructure.AssetSpecifications;
import com.brokerage.common.application.QueryHandler;
import com.brokerage.common.web.PageResponse;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ListAssetsHandler implements QueryHandler<ListAssetsQuery, PageResponse<AssetView>> {

    private final AssetQueryRepository assets;

    public ListAssetsHandler(AssetQueryRepository assets) {
        this.assets = assets;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AssetView> handle(ListAssetsQuery query) {
        return PageResponse.from(
                assets.findAll(AssetSpecifications.matching(query), query.pageable()),
                AssetView::from);
    }
}
