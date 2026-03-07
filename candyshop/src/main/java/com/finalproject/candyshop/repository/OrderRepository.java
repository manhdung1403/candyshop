package com.finalproject.candyshop.repository;

import com.finalproject.candyshop.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByStatus(String status);
    List<Order> findByCustomerNameContainingIgnoreCase(String name);
}