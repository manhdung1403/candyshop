package com.finalproject.candyshop.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import com.finalproject.candyshop.dto.ProductSalesDto;
import com.finalproject.candyshop.entity.Cart;
import com.finalproject.candyshop.entity.CartItem;
import com.finalproject.candyshop.entity.Product;
import com.finalproject.candyshop.entity.Order;
import com.finalproject.candyshop.entity.OrderItem;
import com.finalproject.candyshop.entity.User;
import com.finalproject.candyshop.repository.CartRepository;
import com.finalproject.candyshop.repository.OrderRepository;
import com.finalproject.candyshop.repository.ProductRepository;
import com.finalproject.candyshop.repository.UserRepository;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired private OrderRepository orderRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private com.finalproject.candyshop.repository.OrderItemRepository orderItemRepository;

    @PostMapping("/checkout")
    @Transactional
    public ResponseEntity<?> checkoutOrder(
            @RequestParam Integer userId,
            @RequestParam String recipientName,
            @RequestParam String recipientPhone,
            @RequestParam String recipientAddress,
            @RequestParam String deliveryDate) {

        if (recipientName == null || recipientName.isBlank() || recipientPhone == null || recipientPhone.isBlank() || recipientAddress == null || recipientAddress.isBlank() || deliveryDate == null || deliveryDate.isBlank()) {
            return ResponseEntity.badRequest().body("Vui lòng điền đủ họ tên, số điện thoại, địa chỉ giao hàng và ngày giao hàng.");
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("Người dùng không tồn tại.");

        Cart cart = cartRepository.findByUserUserId(userId).orElse(null);
        if (cart == null || cart.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body("Giỏ hàng trống.");
        }

        // Tổng hợp số lượng theo từng sản phẩm để kiểm tra tồn kho & cập nhật đồng nhất.
        Map<Integer, Integer> qtyByProductId = cart.getItems().stream()
                .filter(i -> i != null && i.getProduct() != null && i.getProduct().getProductId() != null)
                .collect(Collectors.toMap(
                        i -> i.getProduct().getProductId(),
                        i -> i.getQuantity() == null ? 0 : i.getQuantity(),
                        Integer::sum
                ));

        if (qtyByProductId.isEmpty()) {
            return ResponseEntity.badRequest().body("Giỏ hàng không hợp lệ.");
        }

        // Reload products để kiểm tra tồn kho & dùng đúng giá tại thời điểm checkout.
        Map<Integer, Product> productsById = productRepository.findAllById(qtyByProductId.keySet()).stream()
                .collect(Collectors.toMap(Product::getProductId, p -> p));

        for (Map.Entry<Integer, Integer> e : qtyByProductId.entrySet()) {
            Integer productId = e.getKey();
            Integer qty = e.getValue();
            if (qty == null || qty <= 0) {
                return ResponseEntity.badRequest().body("Số lượng sản phẩm không hợp lệ.");
            }

            Product product = productsById.get(productId);
            if (product == null) {
                return ResponseEntity.badRequest().body("Sản phẩm không tồn tại (id=" + productId + ").");
            }

            int stock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
            if (stock < qty) {
                return ResponseEntity.badRequest().body("Hết hàng cho sản phẩm: " + product.getNameProduct());
            }
        }

        LocalDate parsedDeliveryDate;
        try {
            parsedDeliveryDate = LocalDate.parse(deliveryDate);
        } catch (DateTimeParseException ex) {
            return ResponseEntity.badRequest().body("Ngày giao hàng không hợp lệ (định dạng yyyy-MM-dd).\n");
        }

        Order order = new Order();
        order.setUser(user);
        order.setRecipientName(recipientName);
        order.setRecipientPhone(recipientPhone);
        order.setRecipientAddress(recipientAddress);
        order.setDeliveryDate(parsedDeliveryDate);

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Integer, Integer> e : qtyByProductId.entrySet()) {
            Integer productId = e.getKey();
            Integer qty = e.getValue();

            Product product = productsById.get(productId);
            // qtyByProductId đã được validate nên product cũng không null theo logic phía trên.
            if (product == null) continue;

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(qty);
            orderItem.setUnitPrice(product.getPrice());
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(qty));
            orderItem.setSubtotal(subtotal);
            order.addItem(orderItem);
            total = total.add(subtotal);
        }
        order.setTotalAmount(total);

        orderRepository.save(order);

        // Sau khi tạo đơn thành công: cập nhật tồn kho theo số lượng đã mua.
        for (Map.Entry<Integer, Integer> e : qtyByProductId.entrySet()) {
            Integer productId = e.getKey();
            Integer qty = e.getValue();
            Product product = productsById.get(productId);
            if (product == null) continue;

            int stock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
            product.setStockQuantity(stock - qty);
            productRepository.save(product);
        }

        cart.getItems().clear();
        cartRepository.save(cart);

        return ResponseEntity.ok(Map.of("message", "Thanh toán thành công!", "orderId", order.getOrderId()));
    }

    @GetMapping
    public ResponseEntity<?> getOrdersByUser(@RequestParam Integer userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("Người dùng không tồn tại.");

        List<Order> orders = orderRepository.findByUserUserIdOrderByOrderDateDesc(userId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        Long totalOrders = orderRepository.getTotalOrders();
        BigDecimal totalRevenue = orderRepository.getTotalRevenue();
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        // Thống kê bán hàng theo sản phẩm
        List<Object[]> bestByQty = orderItemRepository.findProductSalesByQuantity();
        List<Object[]> bestByRevenue = orderItemRepository.findProductSalesByRevenue();

        long totalProductsSold = bestByQty.stream().mapToLong(r -> r[1] != null ? ((Number) r[1]).longValue() : 0L).sum();

        List<ProductSalesDto> topQuantity = bestByQty.stream().limit(5).map(r -> new ProductSalesDto(
                (String) r[0],
                r[1] != null ? ((Number) r[1]).longValue() : 0L,
                r[2] != null ? (BigDecimal) r[2] : BigDecimal.ZERO
        )).toList();

        List<ProductSalesDto> topRevenue = bestByRevenue.stream().limit(5).map(r -> new ProductSalesDto(
                (String) r[0],
                r[1] != null ? ((Number) r[1]).longValue() : 0L,
                r[2] != null ? (BigDecimal) r[2] : BigDecimal.ZERO
        )).toList();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", totalOrders != null ? totalOrders : 0L);
        stats.put("totalRevenue", totalRevenue);
        stats.put("averageOrder", (totalOrders != null && totalOrders > 0) ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        stats.put("totalProductsSold", totalProductsSold);
        stats.put("topProductsByQuantity", topQuantity);
        stats.put("topProductsByRevenue", topRevenue);

        if (!bestByQty.isEmpty()) {
            Object[] topQty = bestByQty.get(0);
            stats.put("bestSellingProduct", topQty[0]);
        } else {
            stats.put("bestSellingProduct", null);
        }

        if (!bestByRevenue.isEmpty()) {
            Object[] topRev = bestByRevenue.get(0);
            stats.put("highestRevenueProduct", topRev[0]);
        } else {
            stats.put("highestRevenueProduct", null);
        }

        return stats;
    }
}