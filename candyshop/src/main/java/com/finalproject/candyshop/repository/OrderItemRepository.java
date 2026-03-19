package com.finalproject.candyshop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.finalproject.candyshop.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    @Query("SELECT oi.product.nameProduct, SUM(oi.quantity), SUM(oi.subtotal) FROM OrderItem oi GROUP BY oi.product.productId ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> findProductSalesByQuantity();

    @Query("SELECT oi.product.nameProduct, SUM(oi.quantity), SUM(oi.subtotal) FROM OrderItem oi GROUP BY oi.product.productId ORDER BY SUM(oi.subtotal) DESC")
    List<Object[]> findProductSalesByRevenue();
}