package com.bookworm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_items")
@Getter @Setter @NoArgsConstructor
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false) @JoinColumn(name = "order_id")
    private CustomerOrder order;

    private Long bookId;
    private String title;
    private BigDecimal unitPrice;
    private int quantity;
}
