package com.inventory.api;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductRequest(
        @NotBlank String name,
        @NotBlank String sku,
        @NotBlank String category,
        @NotNull @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal unitPrice,
        @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 0) BigDecimal minimumThreshold,
        String description) {
}