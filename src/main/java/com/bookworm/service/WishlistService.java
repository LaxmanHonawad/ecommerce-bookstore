package com.bookworm.service;

import com.bookworm.dto.Dtos.*;
import com.bookworm.exception.ApiException;
import com.bookworm.model.Book;
import com.bookworm.model.WishlistItem;
import com.bookworm.repository.BookRepository;
import com.bookworm.repository.WishlistRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistService {
    private final WishlistRepository wishlist;
    private final BookRepository books;
    private final Mapper mapper;

    @Transactional(readOnly = true)
    public List<BookView> list(Long userId) {
        List<Long> ids = wishlist.findByUserIdOrderByIdDesc(userId).stream().map(WishlistItem::getBookId).toList();
        var byId = new java.util.HashMap<Long, Book>();
        books.findAllById(ids).forEach(b -> byId.put(b.getId(), b));
        return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).map(mapper::book).toList();
    }

    public List<BookView> add(Long userId, Long bookId) {
        if (!books.existsById(bookId)) throw ApiException.notFound("Book not found");
        if (wishlist.findByUserIdAndBookId(userId, bookId).isEmpty()) {
            WishlistItem w = new WishlistItem();
            w.setUserId(userId);
            w.setBookId(bookId);
            wishlist.save(w);
        }
        return list(userId);
    }

    public List<BookView> remove(Long userId, Long bookId) {
        wishlist.findByUserIdAndBookId(userId, bookId).ifPresent(wishlist::delete);
        wishlist.flush();
        return list(userId);
    }
}
