package com.bookworm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Maps table "orders" (class not named Order because ORDER is a JPQL keyword). */
@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor
public class CustomerOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String status;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal deliveryCharge;
    private String couponCode;
    private BigDecimal couponDiscount = BigDecimal.ZERO;
    private int giftPointsUsed;
    private int giftPointsEarned;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private LocalDate estimatedDelivery;
    private Instant createdAt = Instant.now();
    private Instant confirmedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
}
