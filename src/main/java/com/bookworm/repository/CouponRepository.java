package com.bookworm.repository;

import com.bookworm.model.*;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCodeIgnoreCase(String code);
}

