package com.finalproject.candyshop.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemDto {
    private Integer cartItemId;
    private Integer productId;
    private String productName;
    private String imageUrl;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;
}
