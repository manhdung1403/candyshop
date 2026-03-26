package com.finalproject.candyshop.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.finalproject.candyshop.dto.CartDto;
import com.finalproject.candyshop.dto.CartItemDto;
import com.finalproject.candyshop.dto.CartItemRequest;
import com.finalproject.candyshop.entity.Cart;
import com.finalproject.candyshop.entity.CartItem;
import com.finalproject.candyshop.entity.Product;
import com.finalproject.candyshop.entity.User;
import com.finalproject.candyshop.repository.CartItemRepository;
import com.finalproject.candyshop.repository.CartRepository;
import com.finalproject.candyshop.repository.ProductRepository;
import com.finalproject.candyshop.repository.UserRepository;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    private Cart getOrCreateCart(Integer userId) {
        if (userId == null) throw new IllegalArgumentException("userId is required");
        Optional<Cart> cartOpt = cartRepository.findByUserUserId(userId);
        if (cartOpt.isPresent()) return cartOpt.get();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Cart cart = new Cart();
        cart.setUser(user);
        return cartRepository.save(cart);
    }

    @GetMapping
    public ResponseEntity<CartDto> getCart(@RequestParam Integer userId) {
        Cart cart = getOrCreateCart(userId);
        return ResponseEntity.ok(toDto(cart));
    }

    @GetMapping("/count")
    public ResponseEntity<Integer> getCartItemCount(@RequestParam Integer userId) {
        Cart cart = getOrCreateCart(userId);
        int count = cart.getItems().stream().mapToInt(i -> {
            Integer q = i.getQuantity();
            return q == null ? 0 : q;
        }).sum();
        return ResponseEntity.ok(count);
    }

    @PostMapping("/items")
    public ResponseEntity<?> addItemToCart(@RequestBody CartItemRequest request) {
        Cart cart = getOrCreateCart(request.getUserId());
        if (request.getProductId() == null) return ResponseEntity.badRequest().body("Thiếu productId");
        if (request.getQuantity() == null || request.getQuantity() <= 0) request.setQuantity(1);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        Optional<CartItem> existing = cart.getItems().stream()
                .filter(i -> i.getProduct().getProductId().equals(product.getProductId()))
                .findFirst();

        // Tính toán tổng số lượng nếu thêm vào giỏ
        int currentQty = existing.isPresent() ? (existing.get().getQuantity() == null ? 0 : existing.get().getQuantity()) : 0;
        int newQty = currentQty + request.getQuantity();

        // KIỂM TRA TỒN KHO: Chặn nếu vượt quá số lượng kho
        if (product.getStockQuantity() == null || newQty > product.getStockQuantity()) {
            return ResponseEntity.badRequest().body("Vượt quá số lượng tồn kho (" + product.getStockQuantity() + " sản phẩm).");
        }

        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(newQty);
            cartItemRepository.save(item);
        } else {
            CartItem item = new CartItem();
            item.setProduct(product);
            item.setQuantity(request.getQuantity());
            cart.addItem(item);
        }

        cart = cartRepository.save(cart);
        return ResponseEntity.ok(toDto(cart));
    }

    @PutMapping("/items")
    @Transactional
    public ResponseEntity<?> setItemQuantity(@RequestBody CartItemRequest request) {
        try {
            Cart cart = getOrCreateCart(request.getUserId());
            if (request.getProductId() == null) return ResponseEntity.badRequest().body("Thiếu productId.");
            if (request.getQuantity() == null || request.getQuantity() <= 0)
                return ResponseEntity.badRequest().body("Số lượng không hợp lệ.");

            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));

            // KIỂM TRA TỒN KHO: Khi update trực tiếp số lượng trong trang Cart
            if (product.getStockQuantity() == null || request.getQuantity() > product.getStockQuantity()) {
                return ResponseEntity.badRequest().body("Số lượng cập nhật vượt quá tồn kho (" + product.getStockQuantity() + ").");
            }

            Optional<CartItem> existing = cart.getItems().stream()
                    .filter(i -> i.getProduct() != null
                              && i.getProduct().getProductId().equals(request.getProductId()))
                    .findFirst();

            if (existing.isPresent()) {
                CartItem item = existing.get();
                item.setQuantity(request.getQuantity());
                cartItemRepository.save(item);
            } else {
                CartItem item = new CartItem();
                item.setProduct(product);
                item.setQuantity(request.getQuantity());
                cart.addItem(item);
            }

            cart = cartRepository.save(cart);
            return ResponseEntity.ok(toDto(cart));
        } catch (Exception ex) {
            return ResponseEntity.status(500)
                    .body("Lỗi cập nhật số lượng: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    @DeleteMapping("/items")
    @Transactional
    public ResponseEntity<?> removeItem(@RequestParam Integer userId, @RequestParam Integer productId) {
        try {
            Cart cart = getOrCreateCart(userId);

            Optional<CartItem> itemToRemove = cart.getItems().stream()
                    .filter(i -> i.getProduct() != null
                              && i.getProduct().getProductId().equals(productId))
                    .findFirst();

            if (itemToRemove.isPresent()) {
                CartItem item = itemToRemove.get();
                if (cart.getItems() != null) {
                    cart.getItems().remove(item);
                }
                cartItemRepository.delete(item);
                cart = cartRepository.save(cart);
            }

            return ResponseEntity.ok(toDto(cart));
        } catch (Exception ex) {
            return ResponseEntity.status(500)
                    .body("Lỗi xóa sản phẩm: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private CartDto toDto(Cart cart) {
        CartDto dto = new CartDto();
        dto.setCartId(cart.getCartId());
        dto.setUserId(cart.getUser() != null ? cart.getUser().getUserId() : null);

        List<CartItemDto> items = cart.getItems().stream()
                .filter(item -> item != null && item.getProduct() != null)
                .map(item -> {
                    CartItemDto itemDto = new CartItemDto();
                    itemDto.setCartItemId(item.getCartItemId());
                    itemDto.setProductId(item.getProduct().getProductId());
                    itemDto.setProductName(item.getProduct().getNameProduct());
                    itemDto.setImageUrl(item.getProduct().getImageUrl());
                    itemDto.setUnitPrice(item.getProduct().getPrice());
                    itemDto.setQuantity(item.getQuantity());
                    itemDto.setSubtotal(item.getSubtotal());
                    return itemDto;
                }).collect(Collectors.toList());

        dto.setItems(items);

        BigDecimal total = items.stream()
                .map(CartItemDto::getSubtotal)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTotalPrice(total);
        return dto;
    }
}