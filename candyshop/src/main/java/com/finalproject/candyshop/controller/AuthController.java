package com.finalproject.candyshop.controller;

import com.finalproject.candyshop.dto.*;
import com.finalproject.candyshop.entity.*;
import com.finalproject.candyshop.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
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
                        "username", savedUser.getUsername()
                )
        );
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpSession session) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isPresent() && passwordEncoder.matches(request.getPassword(), userOpt.get().getPasswordHash())) {
            return ResponseEntity.ok("Đăng nhập thành công!");
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Sai tài khoản hoặc mật khẩu!");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Chưa đăng nhập!");
        }
        return ResponseEntity.ok(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("Đã đăng xuất!");
    }

    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateUserRequest request, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Chưa đăng nhập!");
        }

        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());

        User updatedUser = userRepository.save(user);
        session.setAttribute("user", updatedUser);
        return ResponseEntity.ok(updatedUser);
    }
}