package com.inventory.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import com.inventory.persistence.entity.Category;

public interface CategoryRepository extends Repository<Category, Long> {
    Optional<Category> findByNameIgnoreCase(String name);

    List<Category> findAllByOrderByNameAsc();
}