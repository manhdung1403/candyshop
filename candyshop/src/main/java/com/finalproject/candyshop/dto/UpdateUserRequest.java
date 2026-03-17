package com.finalproject.candyshop.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String email;
    private String phone;
    private String address;
}
