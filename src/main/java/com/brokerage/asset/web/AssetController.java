package com.brokerage.asset.web;

import com.brokerage.asset.application.AssetView;
import com.brokerage.asset.application.query.ListAssetsHandler;
import com.brokerage.asset.application.query.ListAssetsQuery;
import com.brokerage.common.domain.AssetName;
import com.brokerage.common.domain.CustomerId;
import com.brokerage.common.web.PageResponse;
import com.brokerage.security.AccessPolicy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assets")
@Tag(name = "Assets", description = "Inspect customer holdings")
public class AssetController {

    private final ListAssetsHandler listAssets;
    private final AccessPolicy accessPolicy;

    public AssetController(ListAssetsHandler listAssets, AccessPolicy accessPolicy) {
        this.listAssets = listAssets;
        this.accessPolicy = accessPolicy;
    }

    @GetMapping
    @Operation(summary = "List the assets held by a customer")
    public PageResponse<AssetView> list(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String assetName,
            @RequestParam(defaultValue = "false") boolean nonZeroOnly,
            @ParameterObject @PageableDefault(size = 50, sort = "assetName") Pageable pageable) {

        CustomerId target = accessPolicy.currentScope()
                .resolveTarget(CustomerId.ofNullable(customerId));

        return listAssets.handle(new ListAssetsQuery(
                target,
                assetName == null ? null : AssetName.of(assetName),
                nonZeroOnly,
                pageable));
    }
}
