package com.inventory.product;

public class ProductDuplicateSkuException extends ProductValidationException {
    public ProductDuplicateSkuException() {
        super("SKU is already in use.");
    }
}