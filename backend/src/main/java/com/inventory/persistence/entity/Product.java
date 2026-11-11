package com.inventory.persistence.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "products")
@Check(constraints = "CHAR_LENGTH(name) BETWEEN 1 AND 120"
    + " AND CHAR_LENGTH(sku) BETWEEN 1 AND 64"
    + " AND (description IS NULL OR CHAR_LENGTH(description) <= 2000)"
    + " AND unit_price >= 0 AND minimum_threshold >= 0 AND current_stock >= 0")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 64, columnDefinition = "VARCHAR_IGNORECASE(64)")
    private String sku;

    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "minimum_threshold", nullable = false, precision = 12, scale = 0)
    private BigDecimal minimumThreshold;

    @Column(name = "current_stock", nullable = false, precision = 12, scale = 0)
    private BigDecimal currentStock = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    @Column(nullable = false)
    private long version;

    protected Product() {
    }

    public Product(String name, String sku, Category category, BigDecimal unitPrice,
            BigDecimal minimumThreshold, String description) {
        this.name = name;
        this.sku = sku;
        this.category = category;
        this.unitPrice = unitPrice;
        this.minimumThreshold = minimumThreshold;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getMinimumThreshold() {
        return minimumThreshold;
    }

    public void setMinimumThreshold(BigDecimal minimumThreshold) {
        this.minimumThreshold = minimumThreshold;
    }

    public BigDecimal getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(BigDecimal currentStock) {
        this.currentStock = currentStock;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getVersion() {
        return version;
    }
}