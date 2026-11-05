package com.inventory.stock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.inventory.persistence.entity.Category;
import com.inventory.persistence.entity.Product;
import com.inventory.persistence.entity.StockMovement;
import com.inventory.persistence.repository.CategoryRepository;
import com.inventory.persistence.repository.ProductRepository;
import com.inventory.persistence.repository.StockMovementRepository;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration")
class T06StockRollbackIntegrationTest {
    @Autowired
    private StockService stockService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @MockitoBean
    private StockMovementRepository stockMovementRepository;

    @Test
    void rollsBackBalanceAndVersionWhenMovementInsertFails() {
        Category category = categoryRepository.findByNameIgnoreCase("Electronics").orElseThrow();
        Product product = productRepository.saveAndFlush(new Product("Rollback lamp",
                "T06-" + UUID.randomUUID(), category, BigDecimal.ONE, BigDecimal.ZERO, null));
        when(stockMovementRepository.saveAndFlush(any(StockMovement.class)))
                .thenThrow(new DataIntegrityViolationException("private database detail"));

        assertThrows(DataIntegrityViolationException.class, () -> stockService.recordMovement(product.getId(),
                new StockCommand(StockMovement.Type.IN, BigDecimal.ONE, "Failed receipt", 0L), "inventory-manager"));

        Product persisted = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(BigDecimal.ZERO, persisted.getCurrentStock());
        assertEquals(0L, persisted.getVersion());
        verify(stockMovementRepository).saveAndFlush(any(StockMovement.class));
    }
}