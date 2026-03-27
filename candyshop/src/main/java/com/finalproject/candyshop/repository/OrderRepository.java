package com.finalproject.candyshop.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.finalproject.candyshop.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByUserUserIdOrderByOrderDateDesc(Integer userId);

    List<Order> findByOrderDateBetweenOrderByOrderDateDesc(LocalDateTime start, LocalDateTime end);

    List<Order> findByUserUserIdAndOrderDateBetweenOrderByOrderDateDesc(Integer userId, LocalDateTime start,
            LocalDateTime end);

    @Query("SELECT SUM(o.totalAmount) FROM Order o")
    BigDecimal getTotalRevenue();

    @Query("SELECT COUNT(o) FROM Order o")
    Long getTotalOrders();
}