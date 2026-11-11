package com.inventory.stock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import com.inventory.persistence.entity.Category;
import com.inventory.persistence.entity.Product;
import com.inventory.persistence.entity.StockMovement;
import com.inventory.persistence.repository.ProductRepository;
import com.inventory.persistence.repository.StockMovementRepository;
import com.inventory.product.ProductInactiveException;
import com.inventory.product.ProductNotFoundException;

class T06StockServiceTest {
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final String ACTOR = "inventory-manager";

    private ProductRepository productRepository;
    private StockMovementRepository stockMovementRepository;
    private StockService stockService;
    private Product product;

    @BeforeEach
    void setUp() {
        productRepository = org.mockito.Mockito.mock(ProductRepository.class);
        stockMovementRepository = org.mockito.Mockito.mock(StockMovementRepository.class);
        stockService = new StockService(productRepository, stockMovementRepository);
        product = new Product("Desk lamp", "LAMP-1", new Category("Electronics"),
                new BigDecimal("12.50"), BigDecimal.ZERO, null);
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.saveAndFlush(product)).thenAnswer(invocation -> {
            ReflectionTestUtils.setField(product, "version", product.getVersion() + 1);
            return product;
        });
        when(stockMovementRepository.saveAndFlush(any(StockMovement.class))).thenAnswer(invocation -> {
            StockMovement movement = invocation.getArgument(0);
            ReflectionTestUtils.setField(movement, "id", 1L);
            ReflectionTestUtils.setField(movement, "occurredAt", java.time.Instant.parse("2026-11-04T00:00:00Z"));
            return movement;
        });
    }

    @Test
    void recordsIncomingAndOutgoingMovementWithTrimmedAuditAndUpdatedVersion() {
        StockResponse incoming = stockService.recordMovement(PRODUCT_ID,
                command(StockMovement.Type.IN, "10", "  Restock  ", 0), ACTOR);
        StockResponse outgoing = stockService.recordMovement(PRODUCT_ID,
                command(StockMovement.Type.OUT, "3", "  Issued  ", 1), ACTOR);

        assertEquals(new BigDecimal("10"), incoming.currentStock());
        assertEquals(1L, incoming.version());
        assertEquals("Restock", incoming.reasonReference());
        assertEquals(ACTOR, incoming.actorUsername());
        assertEquals(new BigDecimal("7"), outgoing.currentStock());
        assertEquals(2L, outgoing.version());
        assertEquals("Issued", outgoing.reasonReference());
        verify(stockMovementRepository, org.mockito.Mockito.times(2)).saveAndFlush(any(StockMovement.class));
    }

    @Test
    void acceptsMaximumAndIntegralScaleWithOnlyTrailingZeros() {
        StockResponse response = stockService.recordMovement(PRODUCT_ID,
                command(StockMovement.Type.IN, "999999999999.000", "Opening", 0), ACTOR);

        assertEquals(new BigDecimal("999999999999.000"), response.currentStock());
        assertEquals(1L, response.version());
    }

    @Test
    void rejectsInvalidQuantitiesAndOutOfRangeBalancesBeforePersistence() {
        assertInvalidQuantity("0");
        assertInvalidQuantity("-1");
        assertInvalidQuantity("1.01");
        assertInvalidQuantity("1000000000000");
        assertThrows(StockValidationException.class, () -> stockService.recordMovement(PRODUCT_ID,
                command(StockMovement.Type.OUT, "1", "Issue", 0), ACTOR));
        product.setCurrentStock(new BigDecimal("999999999999"));
        assertThrows(StockValidationException.class, () -> stockService.recordMovement(PRODUCT_ID,
                command(StockMovement.Type.IN, "1", "Overflow", 0), ACTOR));

        verify(productRepository, never()).saveAndFlush(any(Product.class));
        verify(stockMovementRepository, never()).saveAndFlush(any(StockMovement.class));
    }

    @Test
    void rejectsMissingOrStaleVersionWithoutPersistingMovement() {
        assertThrows(StockValidationException.class, () -> stockService.recordMovement(PRODUCT_ID,
                new StockCommand(StockMovement.Type.IN, BigDecimal.ONE, "Restock", null), ACTOR));
        ReflectionTestUtils.setField(product, "version", 4L);
        StockVersionConflictException conflict = assertThrows(StockVersionConflictException.class,
                () -> stockService.recordMovement(PRODUCT_ID,
                        command(StockMovement.Type.IN, "1", "Restock", 3), ACTOR));

        assertTrue(conflict.getMessage().contains("changed since it was read"));
        verify(productRepository, never()).saveAndFlush(any(Product.class));
        verify(stockMovementRepository, never()).saveAndFlush(any(StockMovement.class));
    }

        @Test
        void requiresMovementTypeReasonReferenceAndActor() {
        assertThrows(StockValidationException.class, () -> stockService.recordMovement(PRODUCT_ID,
            new StockCommand(null, BigDecimal.ONE, "Restock", 0L), ACTOR));
        assertThrows(StockValidationException.class, () -> stockService.recordMovement(PRODUCT_ID,
            command(StockMovement.Type.IN, "1", "   ", 0), ACTOR));
        assertThrows(StockValidationException.class, () -> stockService.recordMovement(PRODUCT_ID,
            command(StockMovement.Type.IN, "1", "Restock", 0), "  "));

        verify(productRepository, never()).saveAndFlush(any(Product.class));
        verify(stockMovementRepository, never()).saveAndFlush(any(StockMovement.class));
        }

    @Test
    void rejectsMissingAndInactiveProducts() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class, () -> stockService.recordMovement(PRODUCT_ID,
                command(StockMovement.Type.IN, "1", "Restock", 0), ACTOR));

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        product.setActive(false);
        assertThrows(ProductInactiveException.class, () -> stockService.recordMovement(PRODUCT_ID,
                command(StockMovement.Type.IN, "1", "Restock", 0), ACTOR));
        verify(stockMovementRepository, never()).saveAndFlush(any(StockMovement.class));
    }

    @Test
    void translatesOptimisticPersistenceFailureAndRetainsCause() {
        OptimisticLockingFailureException cause = new OptimisticLockingFailureException("private detail") { };
        org.mockito.Mockito.doThrow(cause).when(productRepository).saveAndFlush(product);

        StockVersionConflictException conflict = assertThrows(StockVersionConflictException.class,
                () -> stockService.recordMovement(PRODUCT_ID,
                        command(StockMovement.Type.IN, "1", "Restock", 0), ACTOR));

        assertEquals(cause, conflict.getCause());
        assertTrue(!conflict.getMessage().contains("private detail"));
        verify(stockMovementRepository, never()).saveAndFlush(any(StockMovement.class));
    }

    private void assertInvalidQuantity(String quantity) {
        assertThrows(StockValidationException.class, () -> stockService.recordMovement(PRODUCT_ID,
                command(StockMovement.Type.IN, quantity, "Restock", 0), ACTOR));
    }

    private StockCommand command(StockMovement.Type type, String quantity, String reason, long version) {
        return new StockCommand(type, new BigDecimal(quantity), reason, version);
    }
}