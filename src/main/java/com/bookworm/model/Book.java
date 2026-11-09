package com.bookworm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "books")
@Getter @Setter @NoArgsConstructor
public class Book {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    private String isbn;

    @ManyToOne(optional = false) @JoinColumn(name = "author_id")
    private Author author;
    @ManyToOne(optional = false) @JoinColumn(name = "publisher_id")
    private Publisher publisher;
    @ManyToOne(optional = false) @JoinColumn(name = "category_id")
    private Category category;

    private String format;
    private String language = "English";
    private BigDecimal price;
    private int stock;
    private BigDecimal ratingAvg = BigDecimal.ZERO;
    private int ratingCount;
    private int soldCount;
    private String coverUrl;
    private LocalDate publishedOn = LocalDate.now();
    private int deliveryDays = 3;
    private Instant createdAt = Instant.now();
}
