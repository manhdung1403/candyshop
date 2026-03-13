package com.finalproject.candyshop.repository;

import com.finalproject.candyshop.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByCartCartId(Integer cartId);
    void deleteByCartCartIdAndProductProductId(Integer cartId, Integer productId);
}
