package com.bookworm.service;

import com.bookworm.dto.Dtos.*;
import com.bookworm.exception.ApiException;
import com.bookworm.model.Book;
import com.bookworm.model.CartItem;
import com.bookworm.model.User;
import com.bookworm.repository.BookRepository;
import com.bookworm.repository.CartItemRepository;
import com.bookworm.repository.UserRepository;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {
    private final CartItemRepository cart;
    private final BookRepository books;
    private final UserRepository users;
    private final PricingService pricing;
    private final Mapper mapper;

    @Transactional(readOnly = true)
    public CartView view(Long userId, String couponCode, Integer giftPoints) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        List<CartItem> items = cart.findByUserIdOrderByIdAsc(userId);
        Map<Long, Book> byId = new HashMap<>();
        books.findAllById(items.stream().map(CartItem::getBookId).toList()).forEach(b -> byId.put(b.getId(), b));
        List<CartItemView> views = new ArrayList<>();
        List<PricingService.Line> lines = new ArrayList<>();
        for (CartItem i : items) {
            Book b = byId.get(i.getBookId());
            if (b == null) continue;
            views.add(new CartItemView(mapper.book(b), i.getQuantity(), b.getPrice().multiply(BigDecimal.valueOf(i.getQuantity()))));
            lines.add(new PricingService.Line(b.getPrice(), i.getQuantity()));
        }
        return new CartView(views, pricing.compute(user, lines, couponCode, giftPoints));
    }

    public CartView add(Long userId, CartItemRequest r) {
        Book b = books.findById(r.bookId()).orElseThrow(() -> ApiException.notFound("Book not found"));
        CartItem item = cart.findByUserIdAndBookId(userId, r.bookId()).orElse(null);
        int newQty = r.quantity() + (item == null ? 0 : item.getQuantity());
        checkStock(b, newQty);
        if (item == null) {
            item = new CartItem();
            item.setUserId(userId);
            item.setBookId(r.bookId());
        }
        item.setQuantity(newQty);
        cart.save(item);
        cart.flush();
        return view(userId, null, null);
    }

    public CartView setQuantity(Long userId, Long bookId, int quantity) {
        CartItem item = cart.findByUserIdAndBookId(userId, bookId).orElseThrow(() -> ApiException.notFound("Item not in cart"));
        checkStock(books.findById(bookId).orElseThrow(() -> ApiException.notFound("Book not found")), quantity);
        item.setQuantity(quantity);
        cart.flush();
        return view(userId, null, null);
    }

    public CartView remove(Long userId, Long bookId) {
        CartItem item = cart.findByUserIdAndBookId(userId, bookId).orElseThrow(() -> ApiException.notFound("Item not in cart"));
        cart.delete(item);
        cart.flush();
        return view(userId, null, null);
    }

    public CartView clear(Long userId) {
        cart.deleteByUserId(userId);
        cart.flush();
        return view(userId, null, null);
    }

    private void checkStock(Book b, int qty) {
        if (qty > b.getStock()) {
            throw ApiException.conflict("Only " + b.getStock() + " copies of '" + b.getTitle() + "' available");
        }
    }
}
