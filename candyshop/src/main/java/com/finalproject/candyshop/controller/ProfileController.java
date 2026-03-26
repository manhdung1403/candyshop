package com.finalproject.candyshop.controller;

import java.util.Map;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.finalproject.candyshop.dto.ProfileUpdateRequest;
import com.finalproject.candyshop.entity.User;
import com.finalproject.candyshop.repository.UserRepository;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<?> getProfile(@RequestParam Integer userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().body("Người dùng không tồn tại.");
            }

            // HashMap cho phép giá trị null (Map.of thì không).
            Map<String, Object> resp = new HashMap<>();
            resp.put("userId", user.getUserId());
            resp.put("username", user.getUsername());
            resp.put("email", user.getEmail());
            resp.put("phone", user.getPhone());
            resp.put("address", user.getAddress());
            resp.put("avatarUrl", user.getAvatarUrl());
            resp.put("roleId", user.getRole() != null ? user.getRole().getRoleId() : null);
            resp.put("roleName", user.getRole() != null ? user.getRole().getRoleName() : null);
            resp.put("isActive", user.getIsActive());
            resp.put("createdAt", user.getCreatedAt());
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            // Trả message để debug nhanh nguyên nhân 500 (thường do mismatch schema DB).
            return ResponseEntity.status(500).body("Lỗi server: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody ProfileUpdateRequest request) {
        if (request.getUserId() == null) {
            return ResponseEntity.badRequest().body("Thiếu userId.");
        }

        User user = userRepository.findById(request.getUserId()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body("Người dùng không tồn tại.");
        }

        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String email = request.getEmail() == null ? "" : request.getEmail().trim();
        String phone = request.getPhone() == null ? null : request.getPhone().trim();
        String address = request.getAddress() == null ? null : request.getAddress().trim();
        String avatarUrl = request.getAvatarUrl() == null ? null : request.getAvatarUrl().trim();
        String newPassword = request.getNewPassword() == null ? "" : request.getNewPassword().trim();

        if (username.isBlank() || email.isBlank()) {
            return ResponseEntity.badRequest().body("Tên đăng nhập và email không được để trống.");
        }

        User existingUsername = userRepository.findByUsername(username).orElse(null);
        if (existingUsername != null && !existingUsername.getUserId().equals(user.getUserId())) {
            return ResponseEntity.badRequest().body("Tên đăng nhập đã tồn tại!");
        }

        User existingEmail = userRepository.findByEmail(email).orElse(null);
        if (existingEmail != null && !existingEmail.getUserId().equals(user.getUserId())) {
            return ResponseEntity.badRequest().body("Email đã được sử dụng!");
        }

        user.setUsername(username);
        user.setEmail(email);
        // Các field còn lại có thể để trống/null.
        user.setPhone((phone != null && !phone.isBlank()) ? phone : null);
        user.setAddress((address != null && !address.isBlank()) ? address : null);
        user.setAvatarUrl((avatarUrl != null && !avatarUrl.isBlank()) ? avatarUrl : null);
        if (!newPassword.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }

        User saved = userRepository.save(user);
        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Cập nhật hồ sơ thành công!");
        resp.put("userId", saved.getUserId());
        resp.put("username", saved.getUsername());
        resp.put("email", saved.getEmail());
        resp.put("phone", saved.getPhone());
        resp.put("address", saved.getAddress());
        resp.put("avatarUrl", saved.getAvatarUrl());
        resp.put("roleId", saved.getRole() != null ? saved.getRole().getRoleId() : null);
        resp.put("roleName", saved.getRole() != null ? saved.getRole().getRoleName() : null);
        return ResponseEntity.ok(resp);
    }
}
