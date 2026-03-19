package com.finalproject.candyshop.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.finalproject.candyshop.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByUserUserIdOrderByOrderDateDesc(Integer userId);

    @Query("SELECT SUM(o.totalAmount) FROM Order o")
    BigDecimal getTotalRevenue();

    @Query("SELECT COUNT(o) FROM Order o")
    Long getTotalOrders();
}