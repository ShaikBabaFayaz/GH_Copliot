package com.inventory.stock;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.OptimisticLockException;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.persistence.entity.Product;
import com.inventory.persistence.entity.StockMovement;
import com.inventory.persistence.repository.ProductRepository;
import com.inventory.persistence.repository.StockMovementRepository;
import com.inventory.product.ProductInactiveException;
import com.inventory.product.ProductNotFoundException;

@Service
@Transactional
public class StockService {
    private static final BigDecimal MAX_STOCK = new BigDecimal("999999999999");

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    public StockService(ProductRepository productRepository, StockMovementRepository stockMovementRepository) {
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    public StockResponse recordMovement(UUID productId, StockCommand command, String actorUsername) {
        ValidatedCommand validated = validate(command, actorUsername);
        Product product = productRepository.findById(productId).orElseThrow(ProductNotFoundException::new);
        if (!product.isActive()) {
            throw new ProductInactiveException();
        }
        if (product.getVersion() != validated.expectedVersion()) {
            throw new StockVersionConflictException();
        }

        BigDecimal nextStock = validated.type() == StockMovement.Type.IN
                ? product.getCurrentStock().add(validated.quantity())
                : product.getCurrentStock().subtract(validated.quantity());
        if (nextStock.signum() < 0 || nextStock.compareTo(MAX_STOCK) > 0) {
            throw new StockValidationException("Resulting stock must be between 0 and 999999999999.");
        }

        product.setCurrentStock(nextStock);
        try {
            Product updatedProduct = productRepository.saveAndFlush(product);
            StockMovement movement = stockMovementRepository.saveAndFlush(new StockMovement(updatedProduct,
                    validated.type(), validated.quantity(), validated.reasonReference(), actorUsername));
            return new StockResponse(updatedProduct.getId(), movement.getId(), movement.getType(),
                    movement.getQuantity(), movement.getReasonReference(), movement.getActorUsername(),
                    movement.getOccurredAt(), updatedProduct.getCurrentStock(), updatedProduct.getVersion());
        } catch (OptimisticLockingFailureException | OptimisticLockException exception) {
            throw new StockVersionConflictException(exception);
        }
    }

    @Transactional(readOnly = true)
    public List<StockMovementView> listMovementHistory(UUID productId) {
        productRepository.findById(productId).orElseThrow(ProductNotFoundException::new);
        return stockMovementRepository.findAllByProduct_IdOrderByOccurredAtAscIdAsc(productId).stream()
                .map(movement -> new StockMovementView(movement.getId(), productId, movement.getType(),
                        movement.getQuantity(), movement.getReasonReference(), movement.getActorUsername(),
                        movement.getOccurredAt()))
                .toList();
    }

    private ValidatedCommand validate(StockCommand command, String actorUsername) {
        if (command == null || command.type() == null) {
            throw new StockValidationException("Movement type is required.");
        }
        BigDecimal quantity = command.quantity();
        if (quantity == null || quantity.signum() <= 0 || quantity.stripTrailingZeros().scale() > 0
                || quantity.compareTo(MAX_STOCK) > 0) {
            throw new StockValidationException("Quantity must be a positive integer no greater than 999999999999.");
        }
        if (command.expectedVersion() == null || command.expectedVersion() < 0) {
            throw new StockValidationException("Expected product version is required.");
        }
        if (command.reasonReference() == null || command.reasonReference().strip().isEmpty()) {
            throw new StockValidationException("Reason/reference is required.");
        }
        if (actorUsername == null || actorUsername.isBlank()) {
            throw new StockValidationException("Actor username is required.");
        }
        return new ValidatedCommand(command.type(), quantity, command.reasonReference().strip(),
                command.expectedVersion());
    }

    private record ValidatedCommand(
            StockMovement.Type type,
            BigDecimal quantity,
            String reasonReference,
            long expectedVersion) {
    }
}