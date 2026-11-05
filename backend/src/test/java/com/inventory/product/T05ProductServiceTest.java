package com.inventory.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.inventory.persistence.entity.Category;
import com.inventory.persistence.entity.Product;
import com.inventory.persistence.repository.CategoryRepository;
import com.inventory.persistence.repository.ProductRepository;

class T05ProductServiceTest {
    private static final UUID PRODUCT_ID = UUID.randomUUID();

    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;
    private ProductService productService;
    private Category electronics;

    @BeforeEach
    void setUp() {
        productRepository = org.mockito.Mockito.mock(ProductRepository.class);
        categoryRepository = org.mockito.Mockito.mock(CategoryRepository.class);
        productService = new ProductService(productRepository, categoryRepository);
        electronics = new Category("Electronics");
        when(categoryRepository.findByNameIgnoreCase("Electronics")).thenReturn(Optional.of(electronics));
    }

    @Test
    void createsNormalizedProductWithZeroInitialStockAndNoOpeningStockInput() {
        ProductCommand command = command("  Desk lamp  ", "  lamp-1 ", " Electronics ",
                "12.5", "4", "  Warm light\n\tsoft glow  ");
        when(productRepository.existsBySku("LAMP-1")).thenReturn(false);
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
            return product;
        });

        ProductResponse response = productService.createProduct(command);

        assertEquals(PRODUCT_ID, response.id());
        assertEquals("Desk lamp", response.name());
        assertEquals("LAMP-1", response.sku());
        assertEquals("Warm light\n\tsoft glow", response.description());
        assertEquals("Electronics", response.category());
        assertEquals(new BigDecimal("12.50"), response.unitPrice());
        assertEquals(new BigDecimal("4"), response.minimumThreshold());
        assertEquals(BigDecimal.ZERO, response.currentStock());
        assertTrue(response.active());
    }

    @Test
    void listsAndReadsActiveProductsOnly() {
        Product activeProduct = product(true);
        when(productRepository.findAllByActiveTrueOrderByNameAsc()).thenReturn(List.of(activeProduct));
        when(productRepository.findByIdAndActiveTrue(PRODUCT_ID)).thenReturn(Optional.of(activeProduct));

        assertEquals(List.of(PRODUCT_ID), productService.listProducts().stream().map(ProductResponse::id).toList());
        assertEquals(PRODUCT_ID, productService.getProduct(PRODUCT_ID).id());
        verify(productRepository).findAllByActiveTrueOrderByNameAsc();
        verify(productRepository).findByIdAndActiveTrue(PRODUCT_ID);
    }

    @Test
    void normalizesCombinedSearchFiltersAndPreservesEmptyResults() {
        Product activeProduct = product(true);
        when(productRepository.searchActiveProducts("lamp-", "Electronics", "LOW_STOCK"))
                .thenReturn(List.of(activeProduct));
        when(productRepository.searchActiveProducts(null, null, null)).thenReturn(List.of());

        assertEquals(List.of(PRODUCT_ID), productService.searchProducts(" lamp- ", " Electronics ",
                ProductStockStatus.LOW_STOCK).stream().map(ProductResponse::id).toList());
        assertTrue(productService.searchProducts("   ", " ", null).isEmpty());

        verify(productRepository).searchActiveProducts("lamp-", "Electronics", "LOW_STOCK");
        verify(productRepository).searchActiveProducts(null, null, null);
    }

    @Test
    void updatesProductFieldsWithoutChangingStock() {
        Product product = product(true);
        when(productRepository.findByIdAndActiveTrue(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.existsBySkuAndIdNot("LAMP-2", PRODUCT_ID)).thenReturn(false);
        when(categoryRepository.findByNameIgnoreCase("Electronics")).thenReturn(Optional.of(electronics));
        when(productRepository.saveAndFlush(product)).thenReturn(product);

        ProductResponse response = productService.updateProduct(PRODUCT_ID,
                command("Table lamp", "lamp-2", "Electronics", "0", "0", null));

        assertEquals("Table lamp", product.getName());
        assertEquals("LAMP-2", product.getSku());
        assertEquals(new BigDecimal("0.00"), product.getUnitPrice());
        assertEquals(BigDecimal.ZERO, product.getMinimumThreshold());
        assertEquals(new BigDecimal("7"), product.getCurrentStock());
        assertEquals(new BigDecimal("7"), response.currentStock());
        assertTrue(product.isActive());
    }

    @Test
    void deactivatesWithoutDeletingAndRejectsRepeatDeactivation() {
        Product product = product(true);
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(productRepository.saveAndFlush(product)).thenReturn(product);

        ProductResponse response = productService.deactivateProduct(PRODUCT_ID);

        assertFalse(response.active());
        assertFalse(product.isActive());
        assertThrows(ProductInactiveException.class, () -> productService.deactivateProduct(PRODUCT_ID));
    }

    @Test
    void rejectsDuplicateSkuAndTranslatesDatabaseRaceToSafeDomainConflict() {
        when(productRepository.existsBySku("LAMP-1")).thenReturn(true);
        ProductValidationException duplicate = assertThrows(ProductValidationException.class,
                () -> productService.createProduct(command("Lamp", "lamp-1", "Electronics", "1", "0", null)));
        assertEquals("SKU is already in use.", duplicate.getMessage());
        verify(productRepository, never()).saveAndFlush(any(Product.class));

        when(productRepository.existsBySku("LAMP-1")).thenReturn(false);
        when(productRepository.saveAndFlush(any(Product.class)))
                .thenThrow(new DataIntegrityViolationException("private database details"));
        ProductDataConflictException conflict = assertThrows(ProductDataConflictException.class,
                () -> productService.createProduct(command("Lamp", "lamp-1", "Electronics", "1", "0", null)));
        assertEquals("Product data conflicts with an existing record.", conflict.getMessage());
        assertFalse(conflict.getMessage().contains("private database details"));
    }

    @Test
    void acceptsUnicodeCodePointBoundariesAndMaximumNumericValues() {
        String name = "😀".repeat(120);
        String description = "é".repeat(2000);
        when(productRepository.existsBySku("SKU")).thenReturn(false);
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
            return product;
        });

        ProductResponse response = productService.createProduct(command(name, "sku", "Electronics",
                "999999999999.99", "999999999999", description));

        assertEquals(120, response.name().codePointCount(0, response.name().length()));
        assertEquals(2000, response.description().codePointCount(0, response.description().length()));
        assertEquals(new BigDecimal("999999999999.99"), response.unitPrice());
        assertEquals(new BigDecimal("999999999999"), response.minimumThreshold());
        assertNotNull(response.id());
    }

    @Test
    void allowsNewlineAndTabInRequiredTextButKeepsSkuFormatStrict() {
        String categoryName = "Office\t&\nStationery";
        Category category = new Category(categoryName);
        when(categoryRepository.findByNameIgnoreCase(categoryName)).thenReturn(Optional.of(category));
        when(productRepository.existsBySku("LAMP-1")).thenReturn(false);
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", PRODUCT_ID);
            return saved;
        });

        ProductResponse response = productService.createProduct(command("Desk\t\nLamp stand", "lamp-1",
                categoryName, "1", "0", null));

        assertEquals("Desk\t\nLamp stand", response.name());
        assertEquals(categoryName, response.category());
        assertThrows(ProductValidationException.class, () -> productService.createProduct(
                command("Desk lamp", "LAMP\t1", categoryName, "1", "0", null)));
    }

    @Test
    void rejectsInvalidRequiredTextSkuDescriptionPriceThresholdAndCategory() {
        assertInvalid(command(" ", "SKU", "Electronics", "1", "0", null));
        assertInvalid(command("x".repeat(121), "SKU", "Electronics", "1", "0", null));
        assertInvalid(command("Lamp", "é", "Electronics", "1", "0", null));
        assertInvalid(command("Lamp", "SKU", "Electronics", "1", "0", "x".repeat(2001)));
        assertInvalid(command("Lamp\r", "SKU", "Electronics", "1", "0", null));
        assertInvalid(command("Lamp\u0000", "SKU", "Electronics", "1", "0", null));
        assertInvalid(command("Lamp\u007f", "SKU", "Electronics", "1", "0", null));
        assertInvalid(command("Lamp\u0085", "SKU", "Electronics", "1", "0", null));
        assertInvalid(command("Lamp", "SKU", "Office\t& Stationery", "1", "0", null));
        assertInvalid(command("Lamp", "SKU", "Electronics", "-0.01", "0", null));
        assertInvalid(command("Lamp", "SKU", "Electronics", "1.001", "0", null));
        assertInvalid(command("Lamp", "SKU", "Electronics", "1000000000000.00", "0", null));
        assertInvalid(command("Lamp", "SKU", "Electronics", "1", "1.5", null));
        assertInvalid(command("Lamp", "SKU", "Electronics", "1", "1000000000000", null));
        when(categoryRepository.findByNameIgnoreCase("Missing")).thenReturn(Optional.empty());
        assertThrows(ProductValidationException.class, () -> productService.createProduct(
                command("Lamp", "SKU", "Missing", "1", "0", null)));
    }

    @Test
    void rejectsMissingProductsAndInactiveUpdates() {
        when(productRepository.findByIdAndActiveTrue(PRODUCT_ID)).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class, () -> productService.getProduct(PRODUCT_ID));
        assertThrows(ProductNotFoundException.class, () -> productService.updateProduct(PRODUCT_ID,
                command("Lamp", "SKU", "Electronics", "1", "0", null)));
    }

    private void assertInvalid(ProductCommand command) {
        assertThrows(ProductValidationException.class, () -> productService.createProduct(command));
    }

    private Product product(boolean active) {
        Product product = new Product("Desk lamp", "LAMP-1", electronics,
                new BigDecimal("12.50"), BigDecimal.ZERO, null);
        product.setCurrentStock(new BigDecimal("7"));
        product.setActive(active);
        ReflectionTestUtils.setField(product, "id", PRODUCT_ID);
        return product;
    }

    private ProductCommand command(String name, String sku, String category,
            String unitPrice, String threshold, String description) {
        return new ProductCommand(name, sku, category, new BigDecimal(unitPrice),
                new BigDecimal(threshold), description);
    }
}