package com.bookworm.repository;

import com.bookworm.model.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {
    List<WishlistItem> findByUserIdOrderByIdDesc(Long userId);
    Optional<WishlistItem> findByUserIdAndBookId(Long userId, Long bookId);
}

