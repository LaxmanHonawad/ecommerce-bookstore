package com.bookworm.controller;

import com.bookworm.dto.Dtos.*;
import com.bookworm.model.User;
import com.bookworm.security.RequireAuth;
import com.bookworm.service.CartService;
import com.bookworm.service.PricingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart, coupons and shipping rate")
public class CartController {
    private final CartService cart;
    private final PricingService pricing;

    /** Optional couponCode / giftPoints query params preview the checkout totals. */
    @RequireAuth
    @GetMapping("/cart")
    public CartView view(@RequestAttribute("user") User user, @RequestParam(required = false) String couponCode,
                         @RequestParam(required = false) Integer giftPoints) {
        return cart.view(user.getId(), couponCode, giftPoints);
    }

    @RequireAuth
    @PostMapping("/cart/items")
    public CartView add(@RequestAttribute("user") User user, @Valid @RequestBody CartItemRequest r) { return cart.add(user.getId(), r); }

    @RequireAuth
    @PutMapping("/cart/items/{bookId}")
    public CartView update(@RequestAttribute("user") User user, @PathVariable Long bookId, @Valid @RequestBody QuantityRequest r) {
        return cart.setQuantity(user.getId(), bookId, r.quantity());
    }

    @RequireAuth
    @DeleteMapping("/cart/items/{bookId}")
    public CartView remove(@RequestAttribute("user") User user, @PathVariable Long bookId) { return cart.remove(user.getId(), bookId); }

    @RequireAuth
    @DeleteMapping("/cart")
    public CartView clear(@RequestAttribute("user") User user) { return cart.clear(user.getId()); }

    @PostMapping("/coupons/validate")
    public CouponValidation validateCoupon(@Valid @RequestBody CouponValidateRequest r) {
        return pricing.validateCoupon(r.code(), r.orderAmount());
    }

    @GetMapping("/shipping/rates")
    public ShippingRate shippingRate(@RequestParam String pin, @RequestParam(defaultValue = "0") BigDecimal orderAmount) {
        return pricing.shippingRate(pin, orderAmount);
    }
}
