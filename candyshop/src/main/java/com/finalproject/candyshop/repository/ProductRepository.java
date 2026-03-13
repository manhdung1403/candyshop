package com.finalproject.candyshop.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.finalproject.candyshop.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByCategoryCategoryId(Integer categoryId);
    List<Product> findByNameProductContainingIgnoreCase(String keyword);
}