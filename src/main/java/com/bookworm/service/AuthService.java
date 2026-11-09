package com.bookworm.service;

import com.bookworm.dto.Dtos.*;
import com.bookworm.exception.ApiException;
import com.bookworm.model.AuthToken;
import com.bookworm.model.User;
import com.bookworm.repository.AuthTokenRepository;
import com.bookworm.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private static final long TOKEN_HOURS = 24;

    private final UserRepository users;
    private final AuthTokenRepository tokens;
    private final Mapper mapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public AuthResponse register(RegisterRequest r) {
        if (users.findByEmailIgnoreCase(r.email().trim()).isPresent()) {
            throw ApiException.conflict("Email is already registered");
        }
        User u = new User();
        u.setName(r.name().trim());
        u.setEmail(r.email().trim());
        u.setPasswordHash(encoder.encode(r.password()));
        u.setRole("CUSTOMER");
        u = users.save(u);
        return issueToken(u);
    }

    public AuthResponse login(LoginRequest r) {
        User u = users.findByEmailIgnoreCase(r.email().trim())
                .filter(x -> encoder.matches(r.password(), x.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return issueToken(u);
    }

    public void logout(String token) {
        tokens.deleteById(token);
    }

    /** Returns the user for a valid, unexpired token, otherwise null. */
    public User resolve(String token) {
        AuthToken t = tokens.findById(token).orElse(null);
        if (t == null) return null;
        if (t.getExpiresAt().isBefore(Instant.now())) {
            tokens.delete(t);
            return null;
        }
        return users.findById(t.getUserId()).orElse(null);
    }

    @Transactional(readOnly = true)
    public UserView me(Long userId) {
        return mapper.user(users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found")));
    }

    private AuthResponse issueToken(User u) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        AuthToken t = new AuthToken();
        t.setToken(HexFormat.of().formatHex(bytes));
        t.setUserId(u.getId());
        t.setExpiresAt(Instant.now().plus(TOKEN_HOURS, ChronoUnit.HOURS));
        tokens.save(t);
        return new AuthResponse(t.getToken(), t.getExpiresAt(), mapper.user(u));
    }
}
