package com.finalproject.candyshop.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.finalproject.candyshop.entity.Role;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByRoleName(String roleName);
}
