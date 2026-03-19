package com.finalproject.candyshop.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductSalesDto {
    private String productName;
    private Long totalQuantity;
    private BigDecimal totalRevenue;
}