package com.finalproject.candyshop.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.finalproject.candyshop.entity.Cart;

public interface CartRepository extends JpaRepository<Cart, Integer> {
    java.util.Optional<Cart> findByUserUserId(Integer userId);
}
