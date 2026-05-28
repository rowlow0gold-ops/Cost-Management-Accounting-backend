package com.demo.cost.dto;

import lombok.Getter;
import lombok.Setter;

public class AuthDtos {

    @Getter @Setter
    public static class LoginRequest {
        private String email;
        private String password;
    }

    @Getter @Setter
    public static class RegisterRequest {
        private String email;
        private String password;
        private String name;
        private String role;
        private Long departmentId;
    }

    @Getter @Setter
    public static class LoginResponse {
        private String token;         // short-lived access token
        private String refreshToken;  // long-lived rotating refresh
        private String email;
        private String name;
        private String role;
        private Long departmentId;
    }

    @Getter @Setter
    public static class RefreshRequest {
        private String refreshToken;
    }

    @Getter @Setter
    public static class TokenPair {
        private String token;
        private String refreshToken;
    }
}
