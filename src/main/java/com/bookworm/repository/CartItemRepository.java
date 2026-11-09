package com.bookworm.repository;

import com.bookworm.model.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUserIdOrderByIdAsc(Long userId);
    Optional<CartItem> findByUserIdAndBookId(Long userId, Long bookId);
    void deleteByUserId(Long userId);
}

