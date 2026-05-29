package com.demo.cost.service;

import com.demo.cost.domain.Role;
import com.demo.cost.domain.User;
import com.demo.cost.dto.AuthDtos.*;
import com.demo.cost.repository.DepartmentRepository;
import com.demo.cost.repository.UserRepository;
import com.demo.cost.security.JwtService;
import com.demo.cost.security.RefreshTokenService;
import com.demo.cost.security.AuditLog;
import com.demo.cost.repository.UserRepository;
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
    private final RefreshTokenService refreshTokens;
    private final AuditLog audit;

    private static final java.util.regex.Pattern PW_LETTER = java.util.regex.Pattern.compile("[A-Za-z]");
    private static final java.util.regex.Pattern PW_DIGIT  = java.util.regex.Pattern.compile("[0-9]");

    private void validatePasswordStrength(String pw) {
        if (pw == null || pw.length() < 12) {
            throw new IllegalArgumentException("비밀번호는 12자 이상이어야 합니다");
        }
        if (!PW_LETTER.matcher(pw).find() || !PW_DIGIT.matcher(pw).find()) {
            throw new IllegalArgumentException("비밀번호는 영문과 숫자를 모두 포함해야 합니다");
        }
    }

    @Transactional
    public LoginResponse register(RegisterRequest req) {
        validatePasswordStrength(req.getPassword());
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
        audit.record(u.getEmail(), "REGISTER", true, "role=" + u.getRole().name());
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
            audit.record(req.getEmail(), "LOGIN_LOCKED", false, "minutes_remaining=" + mins);
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
            audit.record(req.getEmail(), "LOGIN_FAILURE", false, "attempts=" + u.getFailedAttempts());
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        // Success — reset state
        if (u.getFailedAttempts() != 0 || u.getLockedUntil() != null) {
            u.setFailedAttempts(0);
            u.setLockedUntil(null);
            userRepo.save(u);
        }
        LoginResponse resp = toLoginResponse(u);
        audit.record(u.getEmail(), "LOGIN_SUCCESS", true, null);
        return resp;
    }

    private LoginResponse toLoginResponse(User u) {
        LoginResponse r = new LoginResponse();
        r.setToken(jwt.generate(u.getEmail(), u.getRole().name(), u.getId()));
        r.setRefreshToken(refreshTokens.issue(u.getId()));
        r.setEmail(u.getEmail());
        r.setName(u.getName());
        r.setRole(u.getRole().name());
        r.setDepartmentId(u.getDepartment() == null ? null : u.getDepartment().getId());
        return r;
    }

    /** Validate the given refresh token, rotate it, return a new (access, refresh) pair. */
    public TokenPair refresh(String rawRefresh) {
        Long userId = refreshTokens.consume(rawRefresh);
        User u = userRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("User no longer exists"));
        TokenPair pair = new TokenPair();
        pair.setToken(jwt.generate(u.getEmail(), u.getRole().name(), u.getId()));
        pair.setRefreshToken(refreshTokens.issue(u.getId()));
        return pair;
    }

    public void logout(String rawRefresh) {
        if (rawRefresh != null && !rawRefresh.isBlank()) refreshTokens.revoke(rawRefresh);
    }
}
