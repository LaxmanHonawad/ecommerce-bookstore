package com.bookworm.controller;

import com.bookworm.dto.Dtos.*;
import com.bookworm.model.User;
import com.bookworm.security.RequireAuth;
import com.bookworm.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@RequireAuth
@Tag(name = "Orders & Payment", description = "Checkout, payment, order history, buy-again, cancel (48h), return, shipment tracking")
public class OrderController {
    private final OrderService orders;

    @PostMapping("/orders/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView checkout(@RequestAttribute("user") User user, @Valid @RequestBody CheckoutRequest r) { return orders.checkout(user.getId(), r); }

    @GetMapping("/orders")
    public List<OrderView> history(@RequestAttribute("user") User user) { return orders.history(user.getId()); }

    @GetMapping("/orders/buy-again")
    public List<BookView> buyAgain(@RequestAttribute("user") User user) { return orders.buyAgain(user.getId()); }

    @GetMapping("/orders/{id}")
    public OrderView get(@RequestAttribute("user") User user, @PathVariable Long id) { return orders.get(user.getId(), id); }

    @PostMapping("/orders/{id}/payment")
    public PaymentResult pay(@RequestAttribute("user") User user, @PathVariable Long id, @Valid @RequestBody PaymentRequest r) {
        return orders.pay(user.getId(), id, r);
    }

    @GetMapping("/orders/{id}/payments")
    public List<PaymentView> payments(@RequestAttribute("user") User user, @PathVariable Long id) { return orders.payments(user.getId(), id); }

    @PostMapping("/orders/{id}/cancel")
    public OrderView cancel(@RequestAttribute("user") User user, @PathVariable Long id) { return orders.cancel(user.getId(), id); }

    @PostMapping("/orders/{id}/return")
    public OrderView requestReturn(@RequestAttribute("user") User user, @PathVariable Long id) { return orders.requestReturn(user.getId(), id); }

    @GetMapping("/orders/{id}/shipment")
    public ShipmentView shipment(@RequestAttribute("user") User user, @PathVariable Long id) { return orders.shipment(user.getId(), id); }
}
