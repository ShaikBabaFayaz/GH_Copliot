package com.inventory.product;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.persistence.entity.Category;
import com.inventory.persistence.entity.Product;
import com.inventory.persistence.repository.CategoryRepository;
import com.inventory.persistence.repository.ProductRepository;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration")
@Transactional
class T07ProductQueryIntegrationTest {
    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void combinesSearchCategoryAndStockStatusAndClearingFiltersReturnsAllActiveProducts() {
        Category electronics = category("Electronics");
        Category office = category("Office & Stationery");
        Product atThreshold = product("Desk Lamp", "LAMP-001", electronics, "3", "3", true);
        product("Lamp shade", "SHADE-002", office, "8", "2", true);
        product("Lamp inactive", "LAMP-003", electronics, "0", "0", false);
        entityManager.flush();
        entityManager.clear();

        List<ProductResponse> combined = productService.searchProducts(" lamp- ", " electronics ",
                ProductStockStatus.LOW_STOCK);

        assertEquals(List.of(atThreshold.getId()), combined.stream().map(ProductResponse::id).toList());
        assertEquals(2, productService.searchProducts(" lamp ", null, null).size());
        assertEquals(2, productService.searchProducts("   ", " ", null).size());
    }

    @Test
    void lowStockIncludesZeroAndThresholdEqualityButExcludesAboveThresholdAndInactiveProducts() {
        Category electronics = category("Electronics");
        product("Zero", "ZERO-" + UUID.randomUUID(), electronics, "0", "0", true);
        product("Threshold", "THRESHOLD-" + UUID.randomUUID(), electronics, "4", "4", true);
        product("Above", "ABOVE-" + UUID.randomUUID(), electronics, "5", "4", true);
        product("Inactive", "INACTIVE-" + UUID.randomUUID(), electronics, "0", "9", false);
        entityManager.flush();
        entityManager.clear();

        assertEquals(List.of("Threshold", "Zero"), productService.listLowStockProducts().stream()
                .map(ProductResponse::name)
                .toList());
    }

    private Category category(String name) {
        return categoryRepository.findByNameIgnoreCase(name).orElseThrow();
    }

    private Product product(String name, String sku, Category category, String stock, String threshold,
            boolean active) {
        Product product = new Product(name, sku, category, BigDecimal.ONE,
                new BigDecimal(threshold), null);
        product.setCurrentStock(new BigDecimal(stock));
        product.setActive(active);
        return productRepository.saveAndFlush(product);
    }
}