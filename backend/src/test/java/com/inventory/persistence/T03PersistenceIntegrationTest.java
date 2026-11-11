package com.inventory.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.persistence.entity.Category;
import com.inventory.persistence.entity.Product;
import com.inventory.persistence.entity.StockMovement;
import com.inventory.persistence.repository.CategoryRepository;
import com.inventory.persistence.repository.ProductRepository;
import com.inventory.persistence.repository.StockMovementRepository;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration")
@Transactional
class T03PersistenceIntegrationTest {
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void seedsApprovedCategoriesAndPersistsProductDefaultsAndScales() {
        Category category = categoryRepository.findByNameIgnoreCase("electronics").orElseThrow();
        assertEquals("Electronics", category.getName());

        Product product = productRepository.save(new Product("Desk lamp", "LAMP-1", category,
                new BigDecimal("12.5"), new BigDecimal("4"), null));
        entityManager.flush();
        entityManager.clear();

        Product persisted = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(new BigDecimal("12.50"), persisted.getUnitPrice());
        assertEquals(2, persisted.getUnitPrice().scale());
        assertEquals(new BigDecimal("4"), persisted.getMinimumThreshold());
        assertEquals(0, persisted.getMinimumThreshold().scale());
        assertEquals(BigDecimal.ZERO, persisted.getCurrentStock());
        assertEquals(0, persisted.getCurrentStock().scale());
        assertTrue(persisted.isActive());
        assertEquals(0L, persisted.getVersion());
    }

    @Test
        void rejectsCaseVariantSkuUsingDatabaseUniqueIndex() {
        Category category = categoryRepository.findByNameIgnoreCase("Electronics").orElseThrow();
        productRepository.save(new Product("Desk lamp", "Lamp-1", category,
                new BigDecimal("12.50"), BigDecimal.ZERO, null));
        entityManager.flush();

        productRepository.save(new Product("Table lamp", "lAMP-1", category,
                new BigDecimal("15.00"), BigDecimal.ZERO, null));
        PersistenceException exception = assertThrows(PersistenceException.class, entityManager::flush);
        StringBuilder messages = new StringBuilder();
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            messages.append(cause.getMessage()).append(' ');
        }
        assertTrue(messages.toString().toUpperCase(Locale.ROOT).contains("UQ_PRODUCTS_SKU_CI"),
                messages.toString());
    }

    @Test
    void persistsMovementAuditFieldsAndReturnsHistoryChronologically() {
        Category category = categoryRepository.findByNameIgnoreCase("Other").orElseThrow();
        Product product = productRepository.save(new Product("Storage bin", "BIN-1", category,
                new BigDecimal("2.00"), BigDecimal.ZERO, null));
        entityManager.flush();

        stockMovementRepository.save(new StockMovement(product, StockMovement.Type.IN,
                BigDecimal.ONE, "Opening balance", "inventory-manager"));
        stockMovementRepository.save(new StockMovement(product, StockMovement.Type.OUT,
                new BigDecimal("2"), "Issued", "inventory-manager"));
        entityManager.flush();
        entityManager.clear();

        List<StockMovement> history = stockMovementRepository
                .findAllByProduct_IdOrderByOccurredAtAscIdAsc(product.getId());
        assertEquals(2, history.size());
        assertEquals(StockMovement.Type.IN, history.get(0).getType());
        assertEquals(StockMovement.Type.OUT, history.get(1).getType());
        assertNotNull(history.get(0).getOccurredAt());
        assertNotNull(history.get(1).getOccurredAt());
        assertFalse(history.get(0).getOccurredAt().isAfter(history.get(1).getOccurredAt())
                || (history.get(0).getOccurredAt().equals(history.get(1).getOccurredAt())
                        && history.get(0).getId() > history.get(1).getId()));
        assertEquals("Opening balance", history.get(0).getReasonReference());
        assertEquals("inventory-manager", history.get(0).getActorUsername());
    }
}