package com.inventory.product;

public class ProductNotFoundException extends ProductException {
    public ProductNotFoundException() {
        super("Product was not found.");
    }
}