package com.inventory.product;

public class ProductInactiveException extends ProductException {
    public ProductInactiveException() {
        super("Product is inactive.");
    }
}