package com.inventory.stock;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.inventory.persistence.entity.Category;
import com.inventory.persistence.entity.Product;
import com.inventory.persistence.entity.StockMovement;
import com.inventory.persistence.repository.CategoryRepository;
import com.inventory.persistence.repository.ProductRepository;
import com.inventory.persistence.repository.StockMovementRepository;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration")
class T06StockServiceIntegrationTest {
    @Autowired
    private StockService stockService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Test
    void simultaneousOperationsUsingSameVersionProduceOneCommitAndOneConflict() throws Exception {
        Product product = createProduct();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Throwable>> results = new ArrayList<>();
            for (int index = 0; index < 2; index++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Concurrent stock test did not start.");
                    }
                    try {
                        stockService.recordMovement(product.getId(),
                                new StockCommand(StockMovement.Type.IN, BigDecimal.valueOf(4), "Concurrent receipt", 0L),
                                "inventory-manager");
                        return null;
                    } catch (StockVersionConflictException exception) {
                        return exception;
                    }
                }));
            }
            if (!ready.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent stock workers did not become ready.");
            }
            start.countDown();

            List<Throwable> outcomes = new ArrayList<>();
            for (Future<Throwable> result : results) {
                outcomes.add(result.get(10, TimeUnit.SECONDS));
            }

            assertEquals(1, outcomes.stream().filter(java.util.Objects::isNull).count());
            assertEquals(1, outcomes.stream().filter(StockVersionConflictException.class::isInstance).count());
            Product persisted = productRepository.findById(product.getId()).orElseThrow();
            assertEquals(new BigDecimal("4"), persisted.getCurrentStock());
            assertEquals(1L, persisted.getVersion());
            assertEquals(1, stockMovementRepository
                    .findAllByProduct_IdOrderByOccurredAtAscIdAsc(product.getId()).size());
        } finally {
            executor.shutdownNow();
        }
    }

    private Product createProduct() {
        Category category = categoryRepository.findByNameIgnoreCase("Electronics").orElseThrow();
        return productRepository.saveAndFlush(new Product("Concurrent lamp", "T06-" + UUID.randomUUID(), category,
                BigDecimal.ONE, BigDecimal.ZERO, null));
    }
}