package com.inventory.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "stock_movements")
@Check(constraints = "quantity > 0")
public class StockMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 3)
    private Type type;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal quantity;

    @Column(name = "reason_reference", nullable = false)
    @jakarta.persistence.Lob
    private String reasonReference;

    @Column(name = "actor_username", nullable = false)
    @jakarta.persistence.Lob
    private String actorUsername;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected StockMovement() {
    }

    public StockMovement(Product product, Type type, BigDecimal quantity, String reasonReference,
            String actorUsername) {
        this.product = product;
        this.type = type;
        this.quantity = quantity;
        this.reasonReference = reasonReference;
        this.actorUsername = actorUsername;
    }

    @PrePersist
    private void setServerTimestamp() {
        occurredAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public Type getType() {
        return type;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public String getReasonReference() {
        return reasonReference;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public enum Type {
        IN,
        OUT
    }
}