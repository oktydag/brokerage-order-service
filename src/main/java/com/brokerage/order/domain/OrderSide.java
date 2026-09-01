package com.brokerage.order.domain;

import com.brokerage.common.domain.AssetName;

public enum OrderSide {

    BUY {
        @Override
        Reservation outgoingLeg(Order order) {
            return new Reservation(AssetName.TRY, order.totalValue());
        }

        @Override
        Reservation incomingLeg(Order order) {
            return new Reservation(order.getAssetName(), order.getSize());
        }
    },

    SELL {
        @Override
        Reservation outgoingLeg(Order order) {
            return new Reservation(order.getAssetName(), order.getSize());
        }

        @Override
        Reservation incomingLeg(Order order) {
            return new Reservation(AssetName.TRY, order.totalValue());
        }
    };

    abstract Reservation outgoingLeg(Order order);

    abstract Reservation incomingLeg(Order order);
}
