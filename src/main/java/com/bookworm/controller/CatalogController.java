package com.bookworm.controller;

import com.bookworm.dto.Dtos.*;
import com.bookworm.model.User;
import com.bookworm.security.RequireAuth;
import com.bookworm.service.CatalogService;
import com.bookworm.service.WishlistService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Catalog", description = "Home page, categories, brands, search, book details, reviews, wishlist, recommendations")
public class CatalogController {
    private final CatalogService catalog;
    private final WishlistService wishlist;

    /** Landing page sections. Personalised "recommended" when a Bearer token is sent. */
    @GetMapping("/home")
    public Map<String, List<BookView>> home(@RequestAttribute(value = "user", required = false) User user) {
        return catalog.home(user);
    }

    @GetMapping("/categories")
    public List<CategoryView> categories() { return catalog.categories(); }

    @GetMapping("/brands")
    public List<PublisherView> brands() { return catalog.brands(); }

    @GetMapping("/authors")
    public List<AuthorView> authors() { return catalog.authors(); }

    @GetMapping("/authors/{id}")
    public AuthorDetail author(@PathVariable Long id) { return catalog.author(id); }

    @GetMapping("/books")
    public PageView<BookView> books(@RequestParam(required = false) String q,
                                    @RequestParam(required = false) Long categoryId,
                                    @RequestParam(required = false) Long brandId,
                                    @RequestParam(required = false) Long authorId,
                                    @RequestParam(required = false) String language,
                                    @RequestParam(required = false) String format,
                                    @RequestParam(required = false) BigDecimal minPrice,
                                    @RequestParam(required = false) BigDecimal maxPrice,
                                    @RequestParam(defaultValue = "relevance") String sort,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "12") int size) {
        return catalog.search(q, categoryId, brandId, authorId, language, format, minPrice, maxPrice, sort, page, size);
    }

    @GetMapping("/books/{id}")
    public BookDetail book(@PathVariable Long id) { return catalog.detail(id); }

    @GetMapping("/books/{id}/related")
    public List<BookView> related(@PathVariable Long id) { return catalog.related(id, 5); }

    @GetMapping("/books/{id}/reviews")
    public List<ReviewView> reviews(@PathVariable Long id) { return catalog.reviewViews(id); }

    @RequireAuth
    @PostMapping("/books/{id}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewView addReview(@RequestAttribute("user") User user, @PathVariable Long id, @Valid @RequestBody ReviewRequest r) {
        return catalog.addReview(user, id, r);
    }

    @RequireAuth
    @GetMapping("/recommendations")
    public List<BookView> recommendations(@RequestAttribute("user") User user) { return catalog.recommendations(user, 6); }

    @RequireAuth
    @GetMapping("/wishlist")
    public List<BookView> wishlist(@RequestAttribute("user") User user) { return wishlist.list(user.getId()); }

    @RequireAuth
    @PostMapping("/wishlist/{bookId}")
    @ResponseStatus(HttpStatus.CREATED)
    public List<BookView> addWishlist(@RequestAttribute("user") User user, @PathVariable Long bookId) { return wishlist.add(user.getId(), bookId); }

    @RequireAuth
    @DeleteMapping("/wishlist/{bookId}")
    public List<BookView> removeWishlist(@RequestAttribute("user") User user, @PathVariable Long bookId) { return wishlist.remove(user.getId(), bookId); }
}
