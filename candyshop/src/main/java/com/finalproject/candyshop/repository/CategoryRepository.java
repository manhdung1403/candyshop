package com.finalproject.candyshop.repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.finalproject.candyshop.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}