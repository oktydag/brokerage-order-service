package com.brokerage.order.domain;

import com.brokerage.common.domain.valueobjects.Amount;
import com.brokerage.common.domain.valueobjects.AssetName;
import com.brokerage.common.domain.valueobjects.CustomerId;
import com.brokerage.common.domain.valueobjects.Reservation;
import com.brokerage.common.domain.valueobjects.Settlement;
import com.brokerage.order.domain.valueobjects.OrderSide;
import com.brokerage.order.domain.valueobjects.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private CustomerId customerId;

    @Column(name = "asset_name", nullable = false, updatable = false)
    private AssetName assetName;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_side", nullable = false, updatable = false, length = 4)
    private OrderSide orderSide;

    @Column(name = "size", nullable = false, updatable = false)
    private Amount size;

    @Column(name = "price", nullable = false, updatable = false)
    private Amount price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OrderStatus status;

    @Column(name = "create_date", nullable = false, updatable = false)
    private Instant createDate;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Order() {
    }

    private Order(CustomerId customerId, AssetName assetName, OrderSide orderSide,
                  Amount size, Amount price, Instant createDate) {
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.assetName = assetName;
        this.orderSide = orderSide;
        this.size = size;
        this.price = price;
        this.status = OrderStatus.PENDING;
        this.createDate = createDate;
    }

    public static Order place(CustomerId customerId, AssetName assetName, OrderSide orderSide,
                              Amount size, Amount price, Instant createDate) {
        if (assetName.isCurrency()) {
            throw new InvalidOrderException(
                    "%s is the settlement currency and cannot be traded against itself"
                            .formatted(AssetName.TRY));
        }
        if (!size.isPositive()) {
            throw new InvalidOrderException("size must be greater than zero");
        }
        if (!price.isPositive()) {
            throw new InvalidOrderException("price must be greater than zero");
        }
        return new Order(customerId, assetName, orderSide, size, price, createDate);
    }

    public Optional<Reservation> cancel() {
        if (status == OrderStatus.CANCELED) {
            return Optional.empty();
        }
        requirePending(OrderStatus.CANCELED);
        status = OrderStatus.CANCELED;
        return Optional.of(reservation());
    }

    public Optional<Settlement> match() {
        if (status == OrderStatus.MATCHED) {
            return Optional.empty();
        }
        requirePending(OrderStatus.MATCHED);
        status = OrderStatus.MATCHED;
        return Optional.of(settlement());
    }

    public Amount totalValue() {
        return size.multipliedBy(price);
    }

    public Reservation reservation() {
        return orderSide.outgoingLeg(this);
    }

    public Settlement settlement() {
        return new Settlement(orderSide.outgoingLeg(this), orderSide.incomingLeg(this));
    }

    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }

    public boolean belongsTo(CustomerId candidate) {
        return customerId.equals(candidate);
    }

    private void requirePending(OrderStatus target) {
        if (status != OrderStatus.PENDING) {
            throw new IllegalOrderTransitionException(id, status, target);
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

    public OrderSide getOrderSide() {
        return orderSide;
    }

    public Amount getSize() {
        return size;
    }

    public Amount getPrice() {
        return price;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreateDate() {
        return createDate;
    }
}
