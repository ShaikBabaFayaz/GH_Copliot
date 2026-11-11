package com.inventory.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inventory.stock.StockCommand;
import com.inventory.stock.StockMovementView;
import com.inventory.stock.StockResponse;
import com.inventory.stock.StockService;

@RestController
@RequestMapping("/api/v1/products/{productId}/movements")
public class StockController {
    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping
    public StockResponse recordMovement(@PathVariable UUID productId, @Valid @RequestBody MovementRequest request,
            Authentication authentication) {
        return stockService.recordMovement(productId, new StockCommand(request.type(), request.quantity(),
                request.reasonReference(), request.expectedVersion()), authentication.getName());
    }

    @GetMapping
    public List<StockMovementView> listMovementHistory(@PathVariable UUID productId) {
        return stockService.listMovementHistory(productId);
    }
}