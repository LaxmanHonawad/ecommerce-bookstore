package com.bookworm.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "shipments")
@Getter @Setter @NoArgsConstructor
public class Shipment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long orderId;
    private String status;
    private String trackingNumber;
    private LocalDate estimatedDelivery;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}
