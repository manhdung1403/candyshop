package com.finalproject.candyshop.dto;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private Integer userId;
    private String username;
    private String email;
    private String phone;
    private String address;
    private String avatarUrl;
    private String newPassword;
}
