package com.finalproject.candyshop.controller;

import com.finalproject.candyshop.dto.*;
import com.finalproject.candyshop.entity.*;
import com.finalproject.candyshop.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername()))
            return ResponseEntity.badRequest().body("Tên đăng nhập đã tồn tại!");
        if (userRepository.existsByEmail(request.getEmail()))
            return ResponseEntity.badRequest().body("Email đã được sử dụng!");

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword())); // Mã hóa mật khẩu

        Role role = roleRepository.findByRoleName("Customer")
                .orElseThrow(() -> new RuntimeException("Chưa có Role Customer trong DB"));
        user.setRole(role);

        User savedUser = userRepository.save(user);

        // Tạo giỏ hàng ngay khi đăng ký
        Cart cart = new Cart();
        cart.setUser(savedUser);
        cartRepository.save(cart);

        return ResponseEntity.ok(
                java.util.Map.of(
                        "message", "Đăng ký thành công!",
                        "userId", savedUser.getUserId(),
                        "username", savedUser.getUsername(),
                        "roleId", savedUser.getRole() != null ? savedUser.getRole().getRoleId() : null,
                        "roleName", savedUser.getRole() != null ? savedUser.getRole().getRoleName() : null));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        if (userOpt.isPresent() && passwordEncoder.matches(request.getPassword(), userOpt.get().getPasswordHash())) {
            User user = userOpt.get();
            return ResponseEntity.ok(
                    java.util.Map.of(
                            "message", "Đăng nhập thành công!",
                            "userId", user.getUserId(),
                            "username", user.getUsername(),
                            "roleId", user.getRole() != null ? user.getRole().getRoleId() : null,
                            "roleName", user.getRole() != null ? user.getRole().getRoleName() : null));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Sai tài khoản hoặc mật khẩu!");
    }
}