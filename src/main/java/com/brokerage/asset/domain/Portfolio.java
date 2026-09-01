package com.brokerage.asset.domain;

import com.brokerage.common.domain.Amount;
import com.brokerage.common.domain.AssetName;
import com.brokerage.common.domain.CustomerId;
import com.brokerage.common.domain.Reservation;
import com.brokerage.common.domain.Settlement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Portfolio {

    private final CustomerId customerId;
    private final Map<AssetName, Asset> holdings = new LinkedHashMap<>();
    private final List<Asset> newlyCreated = new ArrayList<>();

    private Portfolio(CustomerId customerId, List<Asset> holdings) {
        this.customerId = customerId;
        holdings.forEach(asset -> this.holdings.put(asset.getAssetName(), asset));
    }

    public static Portfolio of(CustomerId customerId, List<Asset> holdings) {
        return new Portfolio(customerId, holdings);
    }

    public static Portfolio empty(CustomerId customerId) {
        return new Portfolio(customerId, List.of());
    }

    public void reserve(Reservation reservation) {
        require(reservation.assetName()).reserve(reservation.amount());
    }

    public void release(Reservation reservation) {
        require(reservation.assetName()).release(reservation.amount());
    }

    public void settle(Settlement settlement) {
        Reservation outgoing = settlement.outgoing();
        Reservation incoming = settlement.incoming();
        require(outgoing.assetName()).debit(outgoing.amount());
        holdingOrOpen(incoming.assetName()).credit(incoming.amount());
    }

    public void deposit(AssetName assetName, Amount amount) {
        holdingOrOpen(assetName).credit(amount);
    }

    public Optional<Asset> holding(AssetName assetName) {
        return Optional.ofNullable(holdings.get(assetName));
    }

    public List<Asset> holdings() {
        return List.copyOf(holdings.values());
    }

    public List<Asset> newlyCreated() {
        return List.copyOf(newlyCreated);
    }

    public CustomerId customerId() {
        return customerId;
    }

    private Asset require(AssetName assetName) {
        Asset asset = holdings.get(assetName);
        if (asset == null) {
            throw new AssetNotHeldException(customerId, assetName);
        }
        return asset;
    }

    private Asset holdingOrOpen(AssetName assetName) {
        return holdings.computeIfAbsent(assetName, name -> {
            Asset opened = Asset.open(customerId, name);
            newlyCreated.add(opened);
            return opened;
        });
    }
}
