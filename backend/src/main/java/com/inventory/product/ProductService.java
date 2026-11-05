package com.inventory.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.persistence.entity.Category;
import com.inventory.persistence.entity.Product;
import com.inventory.persistence.repository.CategoryRepository;
import com.inventory.persistence.repository.ProductRepository;

@Service
@Transactional
public class ProductService {
    private static final Pattern SKU_PATTERN = Pattern.compile("[A-Z0-9][A-Z0-9._/-]{0,63}");
    private static final BigDecimal MAX_PRICE = new BigDecimal("999999999999.99");
    private static final BigDecimal MAX_THRESHOLD = new BigDecimal("999999999999");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public ProductResponse createProduct(ProductCommand command) {
        NormalizedProduct normalized = normalize(command);
        if (productRepository.existsBySku(normalized.sku())) {
            throw duplicateSku();
        }

        Category category = findCategory(normalized.category());
        Product product = new Product(normalized.name(), normalized.sku(), category,
                normalized.unitPrice(), normalized.minimumThreshold(), normalized.description());
        return persist(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID id) {
        return toResponse(findActiveProduct(id));
    }

    @Transactional(readOnly = true)
    public ProductResponse getInactiveProduct(UUID id) {
        Product product = productRepository.findById(id).filter(existing -> !existing.isActive())
                .orElseThrow(ProductNotFoundException::new);
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts() {
        return productRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listInactiveProducts() {
        return productRepository.findAllByActiveFalseOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String query, String category, ProductStockStatus stockStatus) {
        return productRepository.searchActiveProducts(normalizeFilter(query), normalizeFilter(category),
                stockStatus == null ? null : stockStatus.name()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listLowStockProducts() {
        return searchProducts(null, null, ProductStockStatus.LOW_STOCK);
    }

    public ProductResponse updateProduct(UUID id, ProductCommand command) {
        NormalizedProduct normalized = normalize(command);
        Product product = findActiveProduct(id);
        if (productRepository.existsBySkuAndIdNot(normalized.sku(), id)) {
            throw duplicateSku();
        }

        Category category = findCategory(normalized.category());
        product.setName(normalized.name());
        product.setSku(normalized.sku());
        product.setDescription(normalized.description());
        product.setCategory(category);
        product.setUnitPrice(normalized.unitPrice());
        product.setMinimumThreshold(normalized.minimumThreshold());
        return persist(product);
    }

    public ProductResponse deactivateProduct(UUID id) {
        Product product = productRepository.findById(id).orElseThrow(ProductNotFoundException::new);
        if (!product.isActive()) {
            throw new ProductInactiveException();
        }
        product.setActive(false);
        return persist(product);
    }

    private ProductResponse persist(Product product) {
        try {
            return toResponse(productRepository.saveAndFlush(product));
        } catch (DataIntegrityViolationException exception) {
            throw new ProductDataConflictException();
        }
    }

    private Product findActiveProduct(UUID id) {
        return productRepository.findByIdAndActiveTrue(id).orElseThrow(ProductNotFoundException::new);
    }

    private Category findCategory(String name) {
        return categoryRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new ProductValidationException("Category does not exist."));
    }

    private NormalizedProduct normalize(ProductCommand command) {
        if (command == null) {
            throw new ProductValidationException("Product details are required.");
        }

        String name = requiredText(command.name(), "Name");
        int nameLength = name.codePointCount(0, name.length());
        if (nameLength < 1 || nameLength > 120) {
            throw new ProductValidationException("Name must contain 1 to 120 Unicode code points.");
        }

        String sku = requiredText(command.sku(), "SKU").toUpperCase(Locale.ROOT);
        if (sku.length() > 64 || !SKU_PATTERN.matcher(sku).matches()) {
            throw new ProductValidationException("SKU must match the approved 1 to 64 character format.");
        }

        String category = requiredText(command.category(), "Category");
        BigDecimal unitPrice = validatePrice(command.unitPrice());
        BigDecimal minimumThreshold = validateThreshold(command.minimumThreshold());
        String description = normalizeDescription(command.description());
        return new NormalizedProduct(name, sku, category, unitPrice, minimumThreshold, description);
    }

    private String requiredText(String value, String field) {
        if (value == null) {
            throw new ProductValidationException(field + " is required.");
        }
        if (value.codePoints().anyMatch(codePoint -> Character.isISOControl(codePoint)
            && codePoint != '\n' && codePoint != '\t')) {
            throw new ProductValidationException(field + " contains a disallowed control character.");
        }
        String normalized = value.strip();
        if (normalized.isEmpty()) {
            throw new ProductValidationException(field + " is required.");
        }
        return normalized;
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeDescription(String value) {
        if (value == null) {
            return null;
        }
        rejectDisallowedControls(value, "Description");
        String normalized = value.strip();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.codePointCount(0, normalized.length()) > 2000) {
            throw new ProductValidationException("Description must not exceed 2,000 Unicode code points.");
        }
        return normalized;
    }

    private void rejectDisallowedControls(String value, String field) {
        boolean hasDisallowedControl = value.codePoints().anyMatch(codePoint ->
                Character.isISOControl(codePoint) && codePoint != '\n' && codePoint != '\t');
        if (hasDisallowedControl) {
            throw new ProductValidationException(field + " contains a disallowed control character.");
        }
    }

    private BigDecimal validatePrice(BigDecimal value) {
        if (value == null) {
            throw new ProductValidationException("Unit price is required.");
        }
        if (value.signum() < 0 || value.scale() > 2 || value.precision() > 14
                || value.compareTo(MAX_PRICE) > 0) {
            throw new ProductValidationException("Unit price must be INR 0.00 to 999999999999.99 with at most 2 decimals.");
        }
        return value.setScale(2);
    }

    private BigDecimal validateThreshold(BigDecimal value) {
        if (value == null) {
            throw new ProductValidationException("Minimum threshold is required.");
        }
        if (value.signum() < 0 || value.stripTrailingZeros().scale() > 0
                || value.compareTo(MAX_THRESHOLD) > 0) {
            throw new ProductValidationException("Minimum threshold must be an integer from 0 to 999999999999.");
        }
        return value.setScale(0);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getSku(), product.getDescription(),
            product.getCategory().getName(), product.getUnitPrice(), product.getMinimumThreshold(),
            product.getCurrentStock(), product.isActive(), product.getVersion());
    }

    private ProductValidationException duplicateSku() {
        return new ProductDuplicateSkuException();
    }

    private record NormalizedProduct(
            String name,
            String sku,
            String category,
            BigDecimal unitPrice,
            BigDecimal minimumThreshold,
            String description) {
    }
}