package com.bookworm.service;

import com.bookworm.dto.Dtos.*;
import com.bookworm.exception.ApiException;
import com.bookworm.model.Coupon;
import com.bookworm.model.User;
import com.bookworm.repository.CouponRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tax, delivery, coupon, gift-point and shipping-rate rules. */
@Service
@RequiredArgsConstructor
public class PricingService {
    public static final BigDecimal TAX_RATE = new BigDecimal("0.12");
    public static final BigDecimal FREE_DELIVERY_THRESHOLD = new BigDecimal("500");
    public static final BigDecimal DELIVERY_CHARGE = new BigDecimal("40");
    public static final int POINTS_EARN_UNIT = 50;   // 1 gift point per Rs.50 paid; 1 point redeems Rs.1

    public record Line(BigDecimal unitPrice, int quantity) {}

    private final CouponRepository coupons;

    @Transactional(readOnly = true)
    public CouponValidation validateCoupon(String code, BigDecimal orderAmount) {
        Coupon c = coupons.findByCodeIgnoreCase(code.trim()).orElse(null);
        if (c == null) return new CouponValidation(code, false, BigDecimal.ZERO, "Invalid coupon code");
        if (!c.isActive()) return new CouponValidation(c.getCode(), false, BigDecimal.ZERO, "Coupon is not active");
        if (c.getExpiresAt() != null && c.getExpiresAt().isBefore(Instant.now())) {
            return new CouponValidation(c.getCode(), false, BigDecimal.ZERO, "Coupon has expired");
        }
        if (orderAmount.compareTo(c.getMinOrderAmount()) < 0) {
            return new CouponValidation(c.getCode(), false, BigDecimal.ZERO,
                    "Minimum order amount of Rs." + c.getMinOrderAmount().stripTrailingZeros().toPlainString() + " required");
        }
        BigDecimal d = "PERCENT".equals(c.getDiscountType())
                ? orderAmount.multiply(c.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : c.getDiscountValue();
        d = d.min(orderAmount).setScale(2, RoundingMode.HALF_UP);
        return new CouponValidation(c.getCode(), true, d, "Coupon applied");
    }

    @Transactional(readOnly = true)
    public PricingView compute(User user, List<Line> lines, String couponCode, Integer requestedPoints) {
        BigDecimal subtotal = lines.stream()
                .map(l -> l.unitPrice().multiply(BigDecimal.valueOf(l.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        int count = lines.stream().mapToInt(Line::quantity).sum();
        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal delivery = deliveryCharge(subtotal);

        String appliedCode = null;
        BigDecimal discount = BigDecimal.ZERO.setScale(2);
        if (couponCode != null && !couponCode.isBlank()) {
            CouponValidation v = validateCoupon(couponCode, subtotal);
            if (!v.valid()) throw ApiException.badRequest(v.message());
            appliedCode = v.code();
            discount = v.discount();
        }

        BigDecimal payable = subtotal.add(tax).add(delivery).subtract(discount);
        int points = 0;
        if (requestedPoints != null && requestedPoints > 0) {
            points = Math.min(requestedPoints, Math.min(user.getGiftPoints(), payable.intValue()));
            if (requestedPoints > user.getGiftPoints()) {
                throw ApiException.badRequest("Only " + user.getGiftPoints() + " gift points available");
            }
        }
        BigDecimal total = payable.subtract(BigDecimal.valueOf(points)).setScale(2, RoundingMode.HALF_UP);
        int earned = total.divideToIntegralValue(BigDecimal.valueOf(POINTS_EARN_UNIT)).intValue();
        return new PricingView(count, subtotal, tax, delivery, appliedCode, discount, points, total, earned, user.getGiftPoints());
    }

    public BigDecimal deliveryCharge(BigDecimal subtotal) {
        if (subtotal.signum() == 0 || subtotal.compareTo(FREE_DELIVERY_THRESHOLD) >= 0) return BigDecimal.ZERO.setScale(2);
        return DELIVERY_CHARGE.setScale(2);
    }

    /** Simulated rate table: transit days are derived from the first PIN digit. */
    public ShippingRate shippingRate(String pin, BigDecimal orderAmount) {
        if (pin == null || !pin.matches("[0-9]{6}")) throw ApiException.badRequest("pin must be a 6 digit PIN code");
        int days = estimatedDays(pin);
        return new ShippingRate(pin, deliveryCharge(orderAmount), days, LocalDate.now().plusDays(days));
    }

    public int estimatedDays(String pin) {
        return 2 + (pin.charAt(0) - '0') % 4;
    }
}
