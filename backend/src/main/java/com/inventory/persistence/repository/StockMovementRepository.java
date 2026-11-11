package com.inventory.persistence.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import com.inventory.persistence.entity.StockMovement;

public interface StockMovementRepository extends Repository<StockMovement, Long> {
    <S extends StockMovement> S save(S movement);

    <S extends StockMovement> S saveAndFlush(S movement);

    List<StockMovement> findAllByProduct_IdOrderByOccurredAtAscIdAsc(java.util.UUID productId);
}