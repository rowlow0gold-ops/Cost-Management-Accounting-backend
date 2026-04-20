package com.noaats.cost.service;

import com.noaats.cost.domain.Role;
import com.noaats.cost.domain.User;
import com.noaats.cost.dto.AuthDtos.*;
import com.noaats.cost.repository.DepartmentRepository;
import com.noaats.cost.repository.UserRepository;
import com.noaats.cost.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final DepartmentRepository departmentRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    @Transactional
    public LoginResponse register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다");
        }
        User u = User.builder()
            .email(req.getEmail())
            .password(encoder.encode(req.getPassword()))
            .name(req.getName())
            .role(Role.valueOf(req.getRole() == null ? "USER" : req.getRole()))
            .department(req.getDepartmentId() == null ? null
                : departmentRepo.findById(req.getDepartmentId()).orElse(null))
            .build();
        userRepo.save(u);
        return toLoginResponse(u);
    }

    public LoginResponse login(LoginRequest req) {
        User u = userRepo.findByEmail(req.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다"));
        if (!encoder.matches(req.getPassword(), u.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다");
        }
        return toLoginResponse(u);
    }

    private LoginResponse toLoginResponse(User u) {
        LoginResponse r = new LoginResponse();
        r.setToken(jwt.generate(u.getEmail(), u.getRole().name(), u.getId()));
        r.setEmail(u.getEmail());
        r.setName(u.getName());
        r.setRole(u.getRole().name());
        r.setDepartmentId(u.getDepartment() == null ? null : u.getDepartment().getId());
        return r;
    }
}
