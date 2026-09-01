package com.brokerage.common.application;

public interface QueryHandler<Q, R> {

    R handle(Q query);
}
