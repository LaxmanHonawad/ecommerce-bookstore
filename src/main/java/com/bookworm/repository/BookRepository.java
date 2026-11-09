package com.bookworm.repository;

import com.bookworm.model.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;
import org.springframework.data.domain.Pageable;

public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {
    List<Book> findByCategoryIdAndIdNotOrderBySoldCountDesc(Long categoryId, Long id, Pageable page);
    List<Book> findAllByOrderBySoldCountDesc(Pageable page);
    List<Book> findAllByOrderByPublishedOnDesc(Pageable page);
    List<Book> findByCategoryIdInAndIdNotInOrderBySoldCountDesc(Collection<Long> categoryIds, Collection<Long> excludeIds, Pageable page);
    List<Book> findByAuthorId(Long authorId);
    boolean existsByIsbn(String isbn);
}

