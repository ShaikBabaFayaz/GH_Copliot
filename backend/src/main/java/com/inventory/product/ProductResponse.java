package com.inventory.product;

import java.math.BigDecimal;
import java.util.UUID;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

public record ProductResponse(
        UUID id,
        String name,
        String sku,
        String description,
        String category,
        @JsonSerialize(using = ToStringSerializer.class) BigDecimal unitPrice,
        BigDecimal minimumThreshold,
        BigDecimal currentStock,
        boolean active,
        long version) {
}