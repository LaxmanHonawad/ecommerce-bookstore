package com.bookworm.service;

import com.bookworm.dto.Dtos.*;
import com.bookworm.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/** Entity -> DTO conversion. */
@Component
public class Mapper {
    public static final long CANCEL_WINDOW_HOURS = 48;

    public UserView user(User u) {
        return new UserView(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getGiftPoints());
    }

    public AddressView address(Address a) {
        return new AddressView(a.getId(), a.getFirstName(), a.getLastName(), a.getEmail(), a.getPhone(),
                a.getLine1(), a.getLine2(), a.getCity(), a.getPin(), a.getState(), a.getCountry());
    }

    public BookView book(Book b) {
        return new BookView(b.getId(), b.getTitle(), b.getDescription(), b.getIsbn(),
                b.getAuthor().getId(), b.getAuthor().getName(),
                b.getPublisher().getId(), b.getPublisher().getName(),
                b.getCategory().getId(), b.getCategory().getName(),
                b.getFormat(), b.getLanguage(), b.getPrice(), b.getStock(),
                b.getRatingAvg(), b.getRatingCount(), b.getSoldCount(), b.getCoverUrl(),
                b.getPublishedOn(), LocalDate.now().plusDays(b.getDeliveryDays()));
    }

    public OrderView order(CustomerOrder o) {
        var items = o.getItems().stream()
                .map(i -> new OrderItemView(i.getBookId(), i.getTitle(), i.getUnitPrice(), i.getQuantity(),
                        i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))))
                .toList();
        return new OrderView(o.getId(), o.getStatus(), items, o.getSubtotal(), o.getTax(), o.getDeliveryCharge(),
                o.getCouponCode(), o.getCouponDiscount(), o.getGiftPointsUsed(), o.getGiftPointsEarned(),
                o.getTotalAmount(), o.getShippingAddress(), o.getEstimatedDelivery(), o.getCreatedAt(),
                o.getConfirmedAt(), cancellable(o));
    }

    public static boolean cancellable(CustomerOrder o) {
        if ("PENDING_PAYMENT".equals(o.getStatus())) return true;
        return "CONFIRMED".equals(o.getStatus()) && o.getConfirmedAt() != null
                && Instant.now().isBefore(o.getConfirmedAt().plus(CANCEL_WINDOW_HOURS, ChronoUnit.HOURS));
    }

    public PaymentView payment(Payment p) {
        return new PaymentView(p.getId(), p.getOrderId(), p.getMethod(), p.getAmount(), p.getStatus(),
                p.getTransactionRef(), p.getCreatedAt());
    }

    public ShipmentView shipment(Shipment s) {
        return new ShipmentView(s.getOrderId(), s.getStatus(), s.getTrackingNumber(), s.getEstimatedDelivery(),
                s.getUpdatedAt());
    }
}
