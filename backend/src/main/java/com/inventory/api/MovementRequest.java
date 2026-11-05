package com.inventory.api;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import com.inventory.persistence.entity.StockMovement;

public record MovementRequest(
        @NotNull StockMovement.Type type,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 12, fraction = 0) BigDecimal quantity,
        @NotBlank String reasonReference,
        @NotNull @PositiveOrZero Long expectedVersion) {
}