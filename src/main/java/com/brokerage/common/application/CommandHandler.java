package com.brokerage.common.application;

public interface CommandHandler<C, R> {

    R handle(C command);
}
