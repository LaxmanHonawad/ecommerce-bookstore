package com.bookworm.repository;

import com.bookworm.model.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderIdOrderByIdAsc(Long orderId);
}

