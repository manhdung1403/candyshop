package com.finalproject.candyshop.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.finalproject.candyshop.entity.Category;
import com.finalproject.candyshop.entity.Product;
import com.finalproject.candyshop.entity.Role;
import com.finalproject.candyshop.repository.CartItemRepository;
import com.finalproject.candyshop.repository.CategoryRepository;
import com.finalproject.candyshop.repository.OrderItemRepository;
import com.finalproject.candyshop.repository.ProductRepository;
import com.finalproject.candyshop.repository.UserRepository;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private UserRepository userRepository;

    // Lưu vào static/uploads - Spring Boot tự serve /uploads/** không cần config
    // thêm
    private final String UPLOAD_DIR = "src/main/resources/static/uploads/";

    private boolean isAdminUser(Integer userId) {
        if (userId == null)
            return false;
        return userRepository.findById(userId)
                .filter(user -> user.getRole() != null)
                .map(user -> {
                    Role r = user.getRole();
                    return (r.getRoleId() != null && r.getRoleId() == 1) || "Admin".equalsIgnoreCase(r.getRoleName());
                }).orElse(false);
    }

    private ResponseEntity<String> unauthorizedResponse() {
        return ResponseEntity.status(403).body("Chỉ Admin mới được thực hiện hành động này.");
    }

    // GET all products (có thể lọc theo category hoặc keyword)
    @GetMapping
    public List<Product> getAllProducts(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return productRepository.findByNameProductContainingIgnoreCase(keyword);
        }
        if (categoryId != null) {
            return productRepository.findByCategoryCategoryId(categoryId);
        }
        return productRepository.findAll();
    }

    // GET product by ID
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Integer id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST create product (multipart/form-data)
    @PostMapping
    public ResponseEntity<?> createProduct(
            @RequestParam Integer userId,
            @RequestParam String nameProduct,
            @RequestParam BigDecimal price,
            @RequestParam Integer stockQuantity,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String expiryDate,
            @RequestParam(required = false) MultipartFile imageFile) {

        if (!isAdminUser(userId))
            return unauthorizedResponse();

        Product product = new Product();
        product.setNameProduct(nameProduct);
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);

        if (categoryId != null) {
            categoryRepository.findById(categoryId).ifPresent(product::setCategory);
        }
        if (expiryDate != null && !expiryDate.isBlank()) {
            product.setExpiryDate(LocalDate.parse(expiryDate));
        }

        // Xử lý ảnh tải lên
        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = saveImage(imageFile);
            if (imageUrl != null)
                product.setImageUrl(imageUrl);
        }

        return ResponseEntity.ok(productRepository.save(product));
    }

    // PUT update product
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Integer id,
            @RequestParam Integer userId,
            @RequestParam String nameProduct,
            @RequestParam BigDecimal price,
            @RequestParam Integer stockQuantity,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String expiryDate,
            @RequestParam(required = false) MultipartFile imageFile) {

        if (!isAdminUser(userId))
            return unauthorizedResponse();

        Optional<Product> optProduct = productRepository.findById(id);
        if (optProduct.isEmpty())
            return ResponseEntity.notFound().build();

        Product product = optProduct.get();
        product.setNameProduct(nameProduct);
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);

        if (categoryId != null) {
            categoryRepository.findById(categoryId).ifPresent(product::setCategory);
        } else {
            product.setCategory(null);
        }
        if (expiryDate != null && !expiryDate.isBlank()) {
            product.setExpiryDate(LocalDate.parse(expiryDate));
        } else {
            product.setExpiryDate(null);
        }

        // Nếu có ảnh mới thì cập nhật
        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = saveImage(imageFile);
            if (imageUrl != null)
                product.setImageUrl(imageUrl);
        }

        return ResponseEntity.ok(productRepository.save(product));
    }

    // DELETE product
    @DeleteMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> deleteProduct(@PathVariable Integer id,
            @RequestParam Integer userId) {
        if (!isAdminUser(userId))
            return unauthorizedResponse();
        if (!productRepository.existsById(id))
            return ResponseEntity.notFound().build();
        cartItemRepository.deleteByProductProductId(id);
        orderItemRepository.deleteByProductProductId(id);
        productRepository.deleteById(id);
        return ResponseEntity.ok("Đã xóa sản phẩm!");
    }

    // GET all categories
    @GetMapping("/categories")
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    // Hàm lưu ảnh và trả về đường dẫn
    private String saveImage(MultipartFile file) {
        try {
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
            String ext = originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String filename = UUID.randomUUID() + ext;

            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath))
                Files.createDirectories(uploadPath);

            Files.copy(file.getInputStream(), uploadPath.resolve(filename),
                    StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/" + filename;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}