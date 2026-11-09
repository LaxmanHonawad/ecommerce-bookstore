package com.bookworm.repository;

import com.bookworm.model.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByBookIdOrderByCreatedAtDesc(Long bookId);
    Optional<Review> findByBookIdAndUserId(Long bookId, Long userId);
}

