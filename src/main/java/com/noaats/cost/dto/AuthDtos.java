package com.noaats.cost.dto;

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
        private String role;            // ADMIN / MANAGER / USER
        private Long departmentId;
    }

    @Getter @Setter
    public static class LoginResponse {
        private String token;
        private String email;
        private String name;
        private String role;
        private Long departmentId;
    }
}
