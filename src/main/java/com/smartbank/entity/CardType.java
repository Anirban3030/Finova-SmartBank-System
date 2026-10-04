package com.smartbank.entity;

import java.math.BigDecimal;

public enum CardType {
    STANDARD("50000.00"),
    GOLD("150000.00"),
    PLATINUM("500000.00");

    private final BigDecimal defaultLimit;

    CardType(String defaultLimit) {
        this.defaultLimit = new BigDecimal(defaultLimit);
    }

    public BigDecimal getDefaultLimit() {
        return defaultLimit;
    }
}