package com.finalproject.candyshop.controller;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        int count = cart.getItems().stream().mapToInt(i -> i.getQuantity() != null ? i.getQuantity() : 0).sum();
        return ResponseEntity.ok(count);
    }

    @PostMapping("/items")
    public ResponseEntity<CartDto> addOrUpdateItem(@RequestBody CartItemRequest request) {
        Cart cart = getOrCreateCart(request.getUserId());
        if (request.getProductId() == null) return ResponseEntity.badRequest().build();
        if (request.getQuantity() == null || request.getQuantity() <= 0) request.setQuantity(1);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Try update existing line
        Optional<CartItem> existing = cart.getItems().stream()
                .filter(i -> i.getProduct().getProductId().equals(product.getProductId()))
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
    }

    @DeleteMapping("/items")
    public ResponseEntity<CartDto> removeItem(@RequestParam Integer userId, @RequestParam Integer productId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteByCartCartIdAndProductProductId(cart.getCartId(), productId);
        // Refresh cart
        cart = cartRepository.findById(cart.getCartId()).orElse(cart);
        return ResponseEntity.ok(toDto(cart));
    }

    private CartDto toDto(Cart cart) {
        CartDto dto = new CartDto();
        dto.setCartId(cart.getCartId());
        dto.setUserId(cart.getUser() != null ? cart.getUser().getUserId() : null);

        List<CartItemDto> items = cart.getItems().stream().map(item -> {
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
