package com.bookworm.service;

import com.bookworm.dto.Dtos.*;
import com.bookworm.exception.ApiException;
import com.bookworm.model.*;
import com.bookworm.repository.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Checkout, payment (simulated gateway), cancellation, returns, order history and shipment tracking. */
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private static final Pattern EXPIRY = Pattern.compile("(0[1-9]|1[0-2])/(\\d{4})");

    private final OrderRepository orders;
    private final CartItemRepository cart;
    private final BookRepository books;
    private final AddressRepository addresses;
    private final PaymentRepository payments;
    private final ShipmentRepository shipments;
    private final UserRepository users;
    private final PricingService pricing;
    private final CatalogService catalog;
    private final Mapper mapper;

    // ------------------------------------------------------------ checkout
    public OrderView checkout(Long userId, CheckoutRequest r) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        Address addr = addresses.findByIdAndUserId(r.addressId(), userId)
                .orElseThrow(() -> ApiException.badRequest("addressId does not exist for this user"));
        List<CartItem> items = cart.findByUserIdOrderByIdAsc(userId);
        if (items.isEmpty()) throw ApiException.badRequest("Cart is empty");

        Map<Long, Book> byId = new HashMap<>();
        books.findAllById(items.stream().map(CartItem::getBookId).toList()).forEach(b -> byId.put(b.getId(), b));
        List<PricingService.Line> lines = new ArrayList<>();
        int maxDays = pricing.estimatedDays(addr.getPin());
        CustomerOrder o = new CustomerOrder();
        for (CartItem ci : items) {
            Book b = byId.get(ci.getBookId());
            if (b == null) throw ApiException.conflict("A book in your cart is no longer available");
            if (ci.getQuantity() > b.getStock()) {
                throw ApiException.conflict("Only " + b.getStock() + " copies of '" + b.getTitle() + "' available");
            }
            lines.add(new PricingService.Line(b.getPrice(), ci.getQuantity()));
            maxDays = Math.max(maxDays, b.getDeliveryDays());
            OrderItem oi = new OrderItem();
            oi.setOrder(o);
            oi.setBookId(b.getId());
            oi.setTitle(b.getTitle());
            oi.setUnitPrice(b.getPrice());
            oi.setQuantity(ci.getQuantity());
            o.getItems().add(oi);
        }
        PricingView p = pricing.compute(user, lines, r.couponCode(), r.giftPoints());
        o.setUserId(userId);
        o.setStatus("PENDING_PAYMENT");
        o.setSubtotal(p.subtotal());
        o.setTax(p.tax());
        o.setDeliveryCharge(p.deliveryCharge());
        o.setCouponCode(p.couponCode());
        o.setCouponDiscount(p.couponDiscount());
        o.setGiftPointsUsed(p.giftPointsApplied());
        o.setGiftPointsEarned(p.giftPointsEarned());
        o.setTotalAmount(p.totalAmount());
        o.setShippingAddress(format(addr));
        o.setEstimatedDelivery(LocalDate.now().plusDays(maxDays));
        return mapper.order(orders.save(o));
    }

    // ------------------------------------------------------------ payment
    /** Simulated payment gateway. Card numbers ending in 0000 are declined so failures can be tested. */
    public PaymentResult pay(Long userId, Long orderId, PaymentRequest r) {
        CustomerOrder o = mine(userId, orderId);
        if (!"PENDING_PAYMENT".equals(o.getStatus())) throw ApiException.conflict("Order is not awaiting payment (status " + o.getStatus() + ")");
        validatePaymentDetails(r);

        Payment pay = new Payment();
        pay.setOrderId(orderId);
        pay.setMethod(r.method());
        pay.setAmount(o.getTotalAmount());
        pay.setTransactionRef("TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase());

        boolean declined = r.cardNumber() != null && digits(r.cardNumber()).endsWith("0000");
        if (declined) {
            pay.setStatus("FAILED");
            return new PaymentResult(mapper.payment(payments.save(pay)), mapper.order(o));
        }

        // stock and gift points re-checked at the moment of payment
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        if (o.getGiftPointsUsed() > user.getGiftPoints()) throw ApiException.conflict("Gift points are no longer available");
        Map<Long, Book> byId = new HashMap<>();
        books.findAllById(o.getItems().stream().map(OrderItem::getBookId).toList()).forEach(b -> byId.put(b.getId(), b));
        for (OrderItem i : o.getItems()) {
            Book b = byId.get(i.getBookId());
            if (b.getStock() < i.getQuantity()) throw ApiException.conflict("'" + b.getTitle() + "' is now out of stock");
        }
        for (OrderItem i : o.getItems()) {
            Book b = byId.get(i.getBookId());
            b.setStock(b.getStock() - i.getQuantity());
            b.setSoldCount(b.getSoldCount() + i.getQuantity());
        }
        user.setGiftPoints(user.getGiftPoints() - o.getGiftPointsUsed() + o.getGiftPointsEarned());
        o.setStatus("CONFIRMED");
        o.setConfirmedAt(Instant.now());

        Shipment s = new Shipment();
        s.setOrderId(orderId);
        s.setStatus("PROCESSING");
        s.setEstimatedDelivery(o.getEstimatedDelivery());
        pay.setStatus("SUCCESS");
        pay = payments.save(pay);
        s = shipments.save(s);
        s.setTrackingNumber(String.format("BW%010d", orderId));

        cart.deleteByUserId(userId);
        return new PaymentResult(mapper.payment(pay), mapper.order(o));
    }

    private void validatePaymentDetails(PaymentRequest r) {
        switch (r.method()) {
            case "CREDIT_CARD", "DEBIT_CARD" -> {
                if (r.cardNumber() == null || !digits(r.cardNumber()).matches("\\d{16}")) throw ApiException.badRequest("cardNumber must have 16 digits");
                if (r.nameOnCard() == null || r.nameOnCard().isBlank()) throw ApiException.badRequest("nameOnCard is required");
                if (r.cvv() == null || !r.cvv().matches("\\d{3}")) throw ApiException.badRequest("cvv must have 3 digits");
                var m = r.expiry() == null ? null : EXPIRY.matcher(r.expiry());
                if (m == null || !m.matches()) throw ApiException.badRequest("expiry must be in MM/YYYY format");
                if (YearMonth.of(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1))).isBefore(YearMonth.now())) {
                    throw ApiException.badRequest("Card has expired");
                }
            }
            case "UPI" -> {
                if (r.upiId() == null || !r.upiId().matches("[\\w.\\-]{2,}@[\\w]{2,}")) throw ApiException.badRequest("upiId must look like name@bank");
            }
            case "WALLET" -> {
                if (r.walletId() == null || r.walletId().isBlank()) throw ApiException.badRequest("walletId is required");
            }
            default -> throw ApiException.badRequest("Unsupported payment method");
        }
    }

    @Transactional(readOnly = true)
    public List<PaymentView> payments(Long userId, Long orderId) {
        mine(userId, orderId);
        return payments.findByOrderIdOrderByIdAsc(orderId).stream().map(mapper::payment).toList();
    }

    // ------------------------------------------------------------ history
    @Transactional(readOnly = true)
    public List<OrderView> history(Long userId) {
        return orders.findByUserIdOrderByCreatedAtDesc(userId).stream().map(mapper::order).toList();
    }

    @Transactional(readOnly = true)
    public OrderView get(Long userId, Long orderId) {
        return mapper.order(mine(userId, orderId));
    }

    /** "Buy it again": books the user has purchased, most recently bought first. */
    @Transactional(readOnly = true)
    public List<BookView> buyAgain(Long userId) {
        List<Long> ids = orders.findPurchasedBookIds(userId);
        Map<Long, Book> byId = new HashMap<>();
        books.findAllById(ids).forEach(b -> byId.put(b.getId(), b));
        return ids.stream().map(byId::get).filter(Objects::nonNull).map(mapper::book).toList();
    }

    // ------------------------------------------------------------ cancel / return
    public OrderView cancel(Long userId, Long orderId) {
        CustomerOrder o = mine(userId, orderId);
        if (!Mapper.cancellable(o)) {
            throw ApiException.conflict("Order can only be cancelled while pending, or within "
                    + Mapper.CANCEL_WINDOW_HOURS + " hours of confirmation and before shipping (status " + o.getStatus() + ")");
        }
        boolean wasPaid = "CONFIRMED".equals(o.getStatus());
        o.setStatus("CANCELLED");
        if (wasPaid) {
            reverse(o, "CANCELLED");
        }
        return mapper.order(o);
    }

    public OrderView requestReturn(Long userId, Long orderId) {
        CustomerOrder o = mine(userId, orderId);
        if (!"DELIVERED".equals(o.getStatus())) throw ApiException.conflict("Only delivered orders can be returned (status " + o.getStatus() + ")");
        o.setStatus("RETURNED");
        reverse(o, "RETURN_INITIATED");
        return mapper.order(o);
    }

    /** Restocks books, refunds the payment, returns used gift points and removes earned ones. */
    private void reverse(CustomerOrder o, String shipmentStatus) {
        Map<Long, Book> byId = new HashMap<>();
        books.findAllById(o.getItems().stream().map(OrderItem::getBookId).toList()).forEach(b -> byId.put(b.getId(), b));
        for (OrderItem i : o.getItems()) {
            Book b = byId.get(i.getBookId());
            if (b != null) {
                b.setStock(b.getStock() + i.getQuantity());
                b.setSoldCount(Math.max(0, b.getSoldCount() - i.getQuantity()));
            }
        }
        User u = users.findById(o.getUserId()).orElseThrow();
        u.setGiftPoints(Math.max(0, u.getGiftPoints() + o.getGiftPointsUsed() - o.getGiftPointsEarned()));

        payments.findByOrderIdOrderByIdAsc(o.getId()).stream().filter(p -> "SUCCESS".equals(p.getStatus())).findFirst().ifPresent(p -> {
            Payment refund = new Payment();
            refund.setOrderId(o.getId());
            refund.setMethod(p.getMethod());
            refund.setAmount(p.getAmount());
            refund.setStatus("REFUNDED");
            refund.setTransactionRef("RFD-" + p.getTransactionRef().substring(4));
            payments.save(refund);
        });
        shipments.findByOrderId(o.getId()).ifPresent(s -> {
            s.setStatus(shipmentStatus);
            s.setUpdatedAt(Instant.now());
        });
    }

    // ------------------------------------------------------------ shipment
    @Transactional(readOnly = true)
    public ShipmentView shipment(Long userId, Long orderId) {
        mine(userId, orderId);
        return mapper.shipment(shipments.findByOrderId(orderId).orElseThrow(() -> ApiException.notFound("No shipment yet - order is not paid")));
    }

    /** Admin: advance a shipment PROCESSING -> SHIPPED -> DELIVERED. */
    public ShipmentView updateShipment(Long orderId, ShipmentStatusRequest r) {
        CustomerOrder o = orders.findById(orderId).orElseThrow(() -> ApiException.notFound("Order not found"));
        Shipment s = shipments.findByOrderId(orderId).orElseThrow(() -> ApiException.notFound("No shipment for this order"));
        boolean ok = ("SHIPPED".equals(r.status()) && "PROCESSING".equals(s.getStatus()))
                || ("DELIVERED".equals(r.status()) && "SHIPPED".equals(s.getStatus()));
        if (!ok) throw ApiException.conflict("Cannot move shipment from " + s.getStatus() + " to " + r.status());
        s.setStatus(r.status());
        s.setUpdatedAt(Instant.now());
        o.setStatus(r.status());
        return mapper.shipment(s);
    }

    // ------------------------------------------------------------ helpers
    private CustomerOrder mine(Long userId, Long orderId) {
        return orders.findByIdAndUserId(orderId, userId).orElseThrow(() -> ApiException.notFound("Order not found"));
    }

    private static String digits(String s) {
        return s.replaceAll("[\\s-]", "");
    }

    private static String format(Address a) {
        StringBuilder sb = new StringBuilder(a.getFirstName()).append(' ').append(a.getLastName()).append(", ").append(a.getLine1());
        if (a.getLine2() != null && !a.getLine2().isBlank()) sb.append(", ").append(a.getLine2());
        return sb.append(", ").append(a.getCity()).append(", ").append(a.getState()).append(" - ").append(a.getPin())
                .append(", ").append(a.getCountry()).append(", Phone: ").append(a.getPhone()).toString();
    }
}
