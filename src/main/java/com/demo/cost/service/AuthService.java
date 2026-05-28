package com.demo.cost.service;

import com.demo.cost.domain.Role;
import com.demo.cost.domain.User;
import com.demo.cost.dto.AuthDtos.*;
import com.demo.cost.repository.DepartmentRepository;
import com.demo.cost.repository.UserRepository;
import com.demo.cost.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.Duration;
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

    private static final int    LOCKOUT_THRESHOLD = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    @Transactional
    public LoginResponse login(LoginRequest req) {
        User u = userRepo.findByEmail(req.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다"));

        // Account currently locked?
        if (u.getLockedUntil() != null && u.getLockedUntil().isAfter(Instant.now())) {
            long mins = Duration.between(Instant.now(), u.getLockedUntil()).toMinutes() + 1;
            throw new IllegalArgumentException("계정이 잠겼습니다. " + mins + "분 후 다시 시도하세요.");
        }

        if (!encoder.matches(req.getPassword(), u.getPassword())) {
            int attempts = u.getFailedAttempts() + 1;
            u.setFailedAttempts(attempts);
            if (attempts >= LOCKOUT_THRESHOLD) {
                u.setLockedUntil(Instant.now().plus(LOCKOUT_DURATION));
                u.setFailedAttempts(0); // reset counter; lock takes over
            }
            userRepo.save(u);
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        // Success — reset state
        if (u.getFailedAttempts() != 0 || u.getLockedUntil() != null) {
            u.setFailedAttempts(0);
            u.setLockedUntil(null);
            userRepo.save(u);
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
