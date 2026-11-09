package com.bookworm.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** All request/response payloads of the API. */
public final class Dtos {
    private Dtos() {}

    // ---- common
    public record PageView<T>(List<T> items, int page, int size, long totalItems, int totalPages) {}

    // ---- auth / member
    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 6, max = 72) String password) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record UserView(Long id, String name, String email, String role, int giftPoints) {}

    public record AuthResponse(String token, Instant expiresAt, UserView user) {}

    public record AddressRequest(
            @NotBlank @Size(max = 60) String firstName,
            @NotBlank @Size(max = 60) String lastName,
            @Email @Size(max = 150) String email,
            @NotBlank @Pattern(regexp = "\\+?[0-9]{10,15}", message = "must be 10-15 digits, optional leading +") String phone,
            @NotBlank @Size(max = 200) String line1,
            @Size(max = 200) String line2,
            @NotBlank @Size(max = 80) String city,
            @NotBlank @Pattern(regexp = "[0-9]{6}", message = "must be a 6 digit PIN code") String pin,
            @NotBlank @Size(max = 80) String state,
            @NotBlank @Size(max = 80) String country) {}

    public record AddressView(Long id, String firstName, String lastName, String email, String phone,
                              String line1, String line2, String city, String pin, String state, String country) {}

    // ---- catalog
    public record CategoryView(Long id, String name) {}
    public record PublisherView(Long id, String name) {}
    public record AuthorView(Long id, String name, String bio) {}
    public record AuthorDetail(Long id, String name, String bio, List<BookView> books) {}
    public record NameRequest(@NotBlank @Size(max = 80) String name) {}
    public record AuthorRequest(@NotBlank @Size(max = 120) String name, @Size(max = 1000) String bio) {}

    public record BookView(Long id, String title, String description, String isbn,
                           Long authorId, String author, Long publisherId, String publisher,
                           Long categoryId, String category, String format, String language,
                           BigDecimal price, int stock, BigDecimal rating, int ratingCount, int soldCount,
                           String coverUrl, LocalDate publishedOn, LocalDate estimatedDelivery) {}

    public record BookRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            @NotBlank @Size(max = 20) String isbn,
            @NotNull Long authorId,
            @NotNull Long publisherId,
            @NotNull Long categoryId,
            @NotBlank @Pattern(regexp = "Paperback|Hardcover|eBook", message = "must be Paperback, Hardcover or eBook") String format,
            @Size(max = 30) String language,
            @NotNull @DecimalMin("0.01") BigDecimal price,
            @NotNull @Min(0) Integer stock,
            @Size(max = 300) String coverUrl,
            @Min(0) Integer deliveryDays) {}

    public record ReviewRequest(@Min(1) @Max(5) int rating, @Size(max = 100) String comment) {}

    public record ReviewView(Long id, Long bookId, String userName, int rating, String comment, Instant createdAt) {}

    public record BookDetail(BookView book, AuthorView author, List<ReviewView> reviews, List<BookView> relatedBooks) {}

    // ---- cart
    public record CartItemRequest(@NotNull Long bookId, @Min(1) @Max(20) int quantity) {}
    public record QuantityRequest(@Min(1) @Max(20) int quantity) {}

    public record CartItemView(BookView book, int quantity, BigDecimal lineTotal) {}

    public record PricingView(int itemCount, BigDecimal subtotal, BigDecimal tax, BigDecimal deliveryCharge,
                              String couponCode, BigDecimal couponDiscount, int giftPointsApplied,
                              BigDecimal totalAmount, int giftPointsEarned, int giftPointsAvailable) {}

    public record CartView(List<CartItemView> items, PricingView pricing) {}

    // ---- coupons / shipping
    public record CouponValidateRequest(@NotBlank String code, @NotNull @DecimalMin("0") BigDecimal orderAmount) {}
    public record CouponValidation(String code, boolean valid, BigDecimal discount, String message) {}
    public record ShippingRate(String pin, BigDecimal charge, int estimatedDays, LocalDate estimatedDelivery) {}

    // ---- orders / payment / shipment
    public record CheckoutRequest(@NotNull Long addressId, String couponCode, @Min(0) Integer giftPoints) {}

    public record OrderItemView(Long bookId, String title, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {}

    public record OrderView(Long id, String status, List<OrderItemView> items, BigDecimal subtotal, BigDecimal tax,
                            BigDecimal deliveryCharge, String couponCode, BigDecimal couponDiscount,
                            int giftPointsUsed, int giftPointsEarned, BigDecimal totalAmount,
                            String shippingAddress, LocalDate estimatedDelivery, Instant createdAt,
                            Instant confirmedAt, boolean cancellable) {}

    public record PaymentRequest(
            @NotBlank @Pattern(regexp = "CREDIT_CARD|DEBIT_CARD|UPI|WALLET", message = "must be CREDIT_CARD, DEBIT_CARD, UPI or WALLET") String method,
            String cardNumber, String nameOnCard, String cvv, String expiry, String upiId, String walletId) {}

    public record PaymentView(Long id, Long orderId, String method, BigDecimal amount, String status,
                              String transactionRef, Instant createdAt) {}

    public record PaymentResult(PaymentView payment, OrderView order) {}

    public record ShipmentView(Long orderId, String status, String trackingNumber, LocalDate estimatedDelivery,
                               Instant updatedAt) {}

    public record ShipmentStatusRequest(
            @NotBlank @Pattern(regexp = "SHIPPED|DELIVERED", message = "must be SHIPPED or DELIVERED") String status) {}
}
