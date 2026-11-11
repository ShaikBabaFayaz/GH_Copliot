package com.inventory.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.Repository;

import com.inventory.persistence.entity.Product;

public interface ProductRepository extends Repository<Product, UUID> {
    <S extends Product> S save(S product);

    <S extends Product> S saveAndFlush(S product);

    Optional<Product> findById(UUID id);

    Optional<Product> findByIdAndActiveTrue(UUID id);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, UUID id);

    List<Product> findAll();

    List<Product> findAllByActiveTrueOrderByNameAsc();

    List<Product> findAllByActiveFalseOrderByNameAsc();

        @Query("""
            select product from Product product
            where product.active = true
              and (:query is null
               or lower(product.name) like concat('%', lower(:query), '%')
               or lower(product.sku) like concat('%', lower(:query), '%'))
              and (:category is null or lower(product.category.name) = lower(:category))
              and (:stockStatus is null
               or (:stockStatus = 'LOW_STOCK' and product.currentStock <= product.minimumThreshold)
               or (:stockStatus = 'IN_STOCK' and product.currentStock > product.minimumThreshold))
            order by product.name asc
            """)
        List<Product> searchActiveProducts(
            @Param("query") String query,
            @Param("category") String category,
            @Param("stockStatus") String stockStatus);
}