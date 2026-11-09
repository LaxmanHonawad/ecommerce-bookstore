package com.bookworm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "coupons")
@Getter @Setter @NoArgsConstructor
public class Coupon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount = BigDecimal.ZERO;
    private boolean active = true;
    private Instant expiresAt;
}
