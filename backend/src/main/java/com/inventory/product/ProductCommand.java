package com.inventory.product;

import java.math.BigDecimal;

public record ProductCommand(
        String name,
        String sku,
        String category,
        BigDecimal unitPrice,
        BigDecimal minimumThreshold,
        String description) {
}