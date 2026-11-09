package com.bookworm.repository;

import com.bookworm.model.*;
import org.springframework.data.jpa.repository.*;

public interface AuthTokenRepository extends JpaRepository<AuthToken, String> {

}

