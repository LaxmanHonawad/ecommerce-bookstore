package com.bookworm.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "auth_tokens")
@Getter @Setter @NoArgsConstructor
public class AuthToken {
    @Id
    private String token;
    private Long userId;
    private Instant createdAt = Instant.now();
    private Instant expiresAt;
}
