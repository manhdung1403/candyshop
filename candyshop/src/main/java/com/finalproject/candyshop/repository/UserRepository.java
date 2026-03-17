package com.finalproject.candyshop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.finalproject.candyshop.entity.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    Boolean existsByEmail(String email);
}
