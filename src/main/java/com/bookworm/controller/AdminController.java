package com.bookworm.controller;

import com.bookworm.dto.Dtos.*;
import com.bookworm.security.RequireAdmin;
import com.bookworm.service.CatalogService;
import com.bookworm.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequireAdmin
@Tag(name = "Admin", description = "Store / catalog management and shipment updates (ADMIN role)")
public class AdminController {
    private final CatalogService catalog;
    private final OrderService orders;

    @PostMapping("/books")
    @ResponseStatus(HttpStatus.CREATED)
    public BookView createBook(@Valid @RequestBody BookRequest r) { return catalog.createBook(r); }

    @PutMapping("/books/{id}")
    public BookView updateBook(@PathVariable Long id, @Valid @RequestBody BookRequest r) { return catalog.updateBook(id, r); }

    @DeleteMapping("/books/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable Long id) { catalog.deleteBook(id); }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryView createCategory(@Valid @RequestBody NameRequest r) { return catalog.createCategory(r); }

    @PostMapping("/brands")
    @ResponseStatus(HttpStatus.CREATED)
    public PublisherView createBrand(@Valid @RequestBody NameRequest r) { return catalog.createPublisher(r); }

    @PostMapping("/authors")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorView createAuthor(@Valid @RequestBody AuthorRequest r) { return catalog.createAuthor(r); }

    @PutMapping("/orders/{id}/shipment")
    public ShipmentView updateShipment(@PathVariable Long id, @Valid @RequestBody ShipmentStatusRequest r) { return orders.updateShipment(id, r); }
}
