package com.inventory.stock;

import java.math.BigDecimal;

import com.inventory.persistence.entity.StockMovement;

public record StockCommand(
        StockMovement.Type type,
        BigDecimal quantity,
        String reasonReference,
        Long expectedVersion) {
}