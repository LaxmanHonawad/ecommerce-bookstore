package com.bookworm.repository;

import com.bookworm.model.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<CustomerOrder> findByIdAndUserId(Long id, Long userId);

    @Query("select i.bookId from CustomerOrder o join o.items i where o.userId = :uid and o.status in ('CONFIRMED','SHIPPED','DELIVERED') group by i.bookId order by max(o.createdAt) desc")
    List<Long> findPurchasedBookIds(@Param("uid") Long userId);
}

