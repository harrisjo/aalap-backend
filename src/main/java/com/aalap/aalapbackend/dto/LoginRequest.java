package com.aalap.aalapbackend.dto;

import lombok.Data;

@Data
public class LoginRequest {
    String email;
    String password;
}
