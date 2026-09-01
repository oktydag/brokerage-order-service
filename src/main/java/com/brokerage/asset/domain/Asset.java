package com.brokerage.asset.domain;

import com.brokerage.common.domain.Amount;
import com.brokerage.common.domain.AssetName;
import com.brokerage.common.domain.CustomerId;
import com.brokerage.common.domain.InvariantViolationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.UUID;

@Entity
@Table(name = "assets")
public class Asset {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private CustomerId customerId;

    @Column(name = "asset_name", nullable = false, updatable = false)
    private AssetName assetName;

    @Column(name = "size", nullable = false)
    private Amount size;

    @Column(name = "usable_size", nullable = false)
    private Amount usableSize;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Asset() {
    }

    private Asset(CustomerId customerId, AssetName assetName, Amount size, Amount usableSize) {
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.assetName = assetName;
        this.size = size;
        this.usableSize = usableSize;
        checkInvariant();
    }

    static Asset open(CustomerId customerId, AssetName assetName) {
        return new Asset(customerId, assetName, Amount.ZERO, Amount.ZERO);
    }

    void reserve(Amount amount) {
        if (usableSize.isLessThan(amount)) {
            throw new InsufficientUsableBalanceException(customerId, assetName, amount, usableSize);
        }
        usableSize = usableSize.minus(amount);
        checkInvariant();
    }

    void release(Amount amount) {
        usableSize = usableSize.plus(amount);
        checkInvariant();
    }

    void debit(Amount amount) {
        size = size.minus(amount);
        checkInvariant();
    }

    void credit(Amount amount) {
        size = size.plus(amount);
        usableSize = usableSize.plus(amount);
        checkInvariant();
    }

    private void checkInvariant() {
        if (usableSize.isGreaterThan(size)) {
            throw new InvariantViolationException(
                    "usableSize (%s) exceeds size (%s) for %s/%s"
                            .formatted(usableSize, size, customerId, assetName));
        }
    }

    public UUID getId() {
        return id;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public AssetName getAssetName() {
        return assetName;
    }

    public Amount getSize() {
        return size;
    }

    public Amount getUsableSize() {
        return usableSize;
    }

    public Amount getReservedSize() {
        return size.minus(usableSize);
    }
}
