package com.bookworm.repository;

import com.bookworm.model.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserIdOrderByIdDesc(Long userId);
    Optional<Address> findByIdAndUserId(Long id, Long userId);
}

