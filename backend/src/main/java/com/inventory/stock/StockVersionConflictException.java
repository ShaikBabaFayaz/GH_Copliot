package com.inventory.stock;

import com.inventory.product.ProductException;

public class StockVersionConflictException extends ProductException {
    private static final String MESSAGE = "Stock changed since it was read. Reload before retrying.";

    public StockVersionConflictException() {
        super(MESSAGE);
    }

    public StockVersionConflictException(Throwable cause) {
        super(MESSAGE, cause);
    }
}