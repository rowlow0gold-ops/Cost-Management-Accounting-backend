package com.demo.cost.security;

import com.demo.cost.domain.RefreshToken;
import com.demo.cost.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repo;

    @Value("${app.jwt.refresh-expiration-ms:604800000}") // default 7 days
    private long refreshTtlMs;

    private final SecureRandom random = new SecureRandom();

    public String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Issue a fresh refresh token, persist its hash, return the raw value to the caller. */
    @Transactional
    public String issue(Long userId) {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken rt = RefreshToken.builder()
                .tokenHash(hash(raw))
                .userId(userId)
                .expiresAt(Instant.now().plusMillis(refreshTtlMs))
                .revoked(false)
                .createdAt(Instant.now())
                .build();
        repo.save(rt);
        return raw;
    }

    /** Validate raw token, mark it revoked (rotation), return its user id.
     *
     *  REUSE DETECTION: if the token presented has already been revoked, that
     *  almost certainly means it was stolen and replayed. We can't tell which
     *  party (legit user or attacker) used the now-rotated copy, so the safe
     *  thing is to revoke EVERY refresh token for that user. Both parties get
     *  logged out; the legit user re-logs in, the attacker is locked out. */
    @Transactional
    public Long consume(String raw) {
        RefreshToken rt = repo.findByTokenHash(hash(raw))
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        if (rt.isRevoked()) {
            // Replay detected — nuke every active token for this user.
            repo.deleteAllByUserId(rt.getUserId());
            throw new IllegalArgumentException("Refresh token replay detected — all sessions revoked");
        }
        if (rt.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token expired");
        }
        rt.setRevoked(true);
        repo.save(rt);
        return rt.getUserId();
    }

    @Transactional
    public void revoke(String raw) {
        repo.findByTokenHash(hash(raw)).ifPresent(rt -> { rt.setRevoked(true); repo.save(rt); });
    }
}
