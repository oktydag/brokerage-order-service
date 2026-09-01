package com.brokerage.order.domain.valueobjects;

import com.brokerage.common.domain.valueobjects.AssetName;
import com.brokerage.common.domain.valueobjects.Reservation;
import com.brokerage.order.domain.Order;

public enum OrderSide {

    BUY {
        @Override
        public Reservation outgoingLeg(Order order) {
            return new Reservation(AssetName.TRY, order.totalValue());
        }

        @Override
        public Reservation incomingLeg(Order order) {
            return new Reservation(order.getAssetName(), order.getSize());
        }
    },

    SELL {
        @Override
        public Reservation outgoingLeg(Order order) {
            return new Reservation(order.getAssetName(), order.getSize());
        }

        @Override
        public Reservation incomingLeg(Order order) {
            return new Reservation(AssetName.TRY, order.totalValue());
        }
    };

    public abstract Reservation outgoingLeg(Order order);

    public abstract Reservation incomingLeg(Order order);
}
