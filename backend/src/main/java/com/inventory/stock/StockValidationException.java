package com.inventory.stock;

import com.inventory.product.ProductException;

public class StockValidationException extends ProductException {
    public StockValidationException(String message) {
        super(message);
    }
}