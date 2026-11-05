package com.inventory.stock;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.inventory.persistence.entity.StockMovement;

public record StockMovementView(
        Long id,
        UUID productId,
        StockMovement.Type type,
        BigDecimal quantity,
        String reasonReference,
        String actorUsername,
        Instant occurredAt) {
}