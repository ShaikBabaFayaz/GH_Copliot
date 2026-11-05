package com.inventory.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.inventory.product.ProductCommand;
import com.inventory.product.ProductResponse;
import com.inventory.product.ProductService;
import com.inventory.product.ProductStockStatus;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> listProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ProductStockStatus stockStatus) {
        if (query == null && category == null && stockStatus == null) {
            return productService.listProducts();
        }
        return productService.searchProducts(query, category, stockStatus);
    }

    @GetMapping("/inactive")
    public List<ProductResponse> inactiveProducts() {
        return productService.listInactiveProducts();
    }

    @GetMapping("/inactive/{id}")
    public ProductResponse getInactiveProduct(@PathVariable UUID id) {
        return productService.getInactiveProduct(id);
    }

    @GetMapping("/low-stock")
    public List<ProductResponse> lowStockProducts() {
        return productService.listLowStockProducts();
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.createProduct(toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public ProductResponse getProduct(@PathVariable UUID id) {
        return productService.getProduct(id);
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return productService.updateProduct(id, toCommand(request));
    }

    @PatchMapping("/{id}/deactivation")
    public ProductResponse deactivateProduct(@PathVariable UUID id) {
        return productService.deactivateProduct(id);
    }

    private ProductCommand toCommand(ProductRequest request) {
        return new ProductCommand(request.name(), request.sku(), request.category(), request.unitPrice(),
                request.minimumThreshold(), request.description());
    }
}