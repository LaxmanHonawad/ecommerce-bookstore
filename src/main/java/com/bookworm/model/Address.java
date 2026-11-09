package com.bookworm.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "addresses")
@Getter @Setter @NoArgsConstructor
public class Address {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String line1;
    private String line2;
    private String city;
    private String pin;
    private String state;
    private String country;
    private Instant createdAt = Instant.now();
}
