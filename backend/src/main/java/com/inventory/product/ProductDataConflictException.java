package com.inventory.product;

public class ProductDataConflictException extends ProductException {
    public ProductDataConflictException() {
        super("Product data conflicts with an existing record.");
    }
}