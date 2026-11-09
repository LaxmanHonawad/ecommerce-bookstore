package com.bookworm.service;

import com.bookworm.dto.Dtos.*;
import com.bookworm.exception.ApiException;
import com.bookworm.model.*;
import com.bookworm.repository.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CatalogService {
    private final BookRepository books;
    private final CategoryRepository categories;
    private final AuthorRepository authors;
    private final PublisherRepository publishers;
    private final ReviewRepository reviews;
    private final UserRepository users;
    private final OrderRepository orders;
    private final Mapper mapper;

    // ------------------------------------------------------------ browse
    @Transactional(readOnly = true)
    public List<CategoryView> categories() {
        return categories.findAll(Sort.by("name")).stream().map(c -> new CategoryView(c.getId(), c.getName())).toList();
    }

    @Transactional(readOnly = true)
    public List<PublisherView> brands() {
        return publishers.findAll(Sort.by("name")).stream().map(p -> new PublisherView(p.getId(), p.getName())).toList();
    }

    @Transactional(readOnly = true)
    public List<AuthorView> authors() {
        return authors.findAll(Sort.by("name")).stream().map(a -> new AuthorView(a.getId(), a.getName(), a.getBio())).toList();
    }

    @Transactional(readOnly = true)
    public AuthorDetail author(Long id) {
        Author a = authors.findById(id).orElseThrow(() -> ApiException.notFound("Author not found"));
        return new AuthorDetail(a.getId(), a.getName(), a.getBio(),
                books.findByAuthorId(id).stream().map(mapper::book).toList());
    }

    @Transactional(readOnly = true)
    public PageView<BookView> search(String q, Long categoryId, Long brandId, Long authorId, String language,
                                     String format, BigDecimal minPrice, BigDecimal maxPrice, String sort,
                                     int page, int size) {
        Specification<Book> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                Join<Book, Author> a = root.join("author", JoinType.LEFT);
                ps.add(cb.or(cb.like(cb.lower(root.get("title")), like), cb.like(cb.lower(a.get("name")), like)));
            }
            if (categoryId != null) ps.add(cb.equal(root.get("category").get("id"), categoryId));
            if (brandId != null) ps.add(cb.equal(root.get("publisher").get("id"), brandId));
            if (authorId != null) ps.add(cb.equal(root.get("author").get("id"), authorId));
            if (language != null && !language.isBlank()) ps.add(cb.equal(cb.lower(root.get("language")), language.trim().toLowerCase()));
            if (format != null && !format.isBlank()) ps.add(cb.equal(cb.lower(root.get("format")), format.trim().toLowerCase()));
            if (minPrice != null) ps.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            if (maxPrice != null) ps.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Sort s = switch (sort == null ? "relevance" : sort) {
            case "price_asc" -> Sort.by("price");
            case "price_desc" -> Sort.by("price").descending();
            case "rating" -> Sort.by("ratingAvg").descending();
            case "newest" -> Sort.by("publishedOn").descending();
            case "relevance", "bestseller" -> Sort.by("soldCount").descending();
            default -> throw ApiException.badRequest("sort must be one of relevance, price_asc, price_desc, rating, newest, bestseller");
        };
        s = s.and(Sort.by("id"));
        Page<Book> result = books.findAll(spec, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), s));
        return new PageView<>(result.getContent().stream().map(mapper::book).toList(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public BookDetail detail(Long id) {
        Book b = find(id);
        Author a = b.getAuthor();
        return new BookDetail(mapper.book(b), new AuthorView(a.getId(), a.getName(), a.getBio()),
                reviewViews(id), related(id, 5));
    }

    @Transactional(readOnly = true)
    public List<BookView> related(Long id, int limit) {
        Book b = find(id);
        return books.findByCategoryIdAndIdNotOrderBySoldCountDesc(b.getCategory().getId(), id, PageRequest.of(0, limit))
                .stream().map(mapper::book).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, List<BookView>> home(User user) {
        Map<String, List<BookView>> m = new LinkedHashMap<>();
        m.put("recommended", user != null ? recommendations(user, 6)
                : books.findAll(PageRequest.of(0, 6, Sort.by("ratingAvg").descending().and(Sort.by("id")))).stream().map(mapper::book).toList());
        m.put("bestsellers", books.findAllByOrderBySoldCountDesc(PageRequest.of(0, 6)).stream().map(mapper::book).toList());
        m.put("newLaunches", books.findAllByOrderByPublishedOnDesc(PageRequest.of(0, 6)).stream().map(mapper::book).toList());
        return m;
    }

    /** Books from categories the user already bought from (not yet purchased); falls back to bestsellers. */
    @Transactional(readOnly = true)
    public List<BookView> recommendations(User user, int limit) {
        List<Long> purchased = orders.findPurchasedBookIds(user.getId());
        List<Book> result = new ArrayList<>();
        if (!purchased.isEmpty()) {
            Set<Long> cats = new LinkedHashSet<>();
            books.findAllById(purchased).forEach(b -> cats.add(b.getCategory().getId()));
            result = books.findByCategoryIdInAndIdNotInOrderBySoldCountDesc(cats, purchased, PageRequest.of(0, limit));
        }
        if (result.size() < limit) {
            Set<Long> have = new HashSet<>(purchased);
            result.forEach(b -> have.add(b.getId()));
            for (Book b : books.findAllByOrderBySoldCountDesc(PageRequest.of(0, limit + have.size()))) {
                if (result.size() >= limit) break;
                if (have.add(b.getId())) result.add(b);
            }
        }
        return result.stream().map(mapper::book).toList();
    }

    // ------------------------------------------------------------ reviews
    @Transactional(readOnly = true)
    public List<ReviewView> reviewViews(Long bookId) {
        find(bookId);
        List<Review> list = reviews.findByBookIdOrderByCreatedAtDesc(bookId);
        Map<Long, String> names = new HashMap<>();
        users.findAllById(list.stream().map(Review::getUserId).toList()).forEach(u -> names.put(u.getId(), u.getName()));
        return list.stream().map(r -> new ReviewView(r.getId(), r.getBookId(), names.getOrDefault(r.getUserId(), "Reader"),
                r.getRating(), r.getComment(), r.getCreatedAt())).toList();
    }

    public ReviewView addReview(User user, Long bookId, ReviewRequest req) {
        Book b = find(bookId);
        Review r = reviews.findByBookIdAndUserId(bookId, user.getId()).orElse(null);
        BigDecimal total = b.getRatingAvg().multiply(BigDecimal.valueOf(b.getRatingCount()));
        if (r == null) {
            r = new Review();
            r.setBookId(bookId);
            r.setUserId(user.getId());
            total = total.add(BigDecimal.valueOf(req.rating()));
            b.setRatingCount(b.getRatingCount() + 1);
        } else {
            total = total.subtract(BigDecimal.valueOf(r.getRating())).add(BigDecimal.valueOf(req.rating()));
        }
        r.setRating(req.rating());
        r.setComment(req.comment());
        r = reviews.save(r);
        b.setRatingAvg(total.divide(BigDecimal.valueOf(b.getRatingCount()), 2, RoundingMode.HALF_UP).min(BigDecimal.valueOf(5)));
        return new ReviewView(r.getId(), bookId, user.getName(), r.getRating(), r.getComment(), r.getCreatedAt());
    }

    // ------------------------------------------------------------ admin (store / catalog management)
    public BookView createBook(BookRequest r) {
        if (books.existsByIsbn(r.isbn().trim())) throw ApiException.conflict("ISBN already exists");
        Book b = new Book();
        apply(b, r);
        return mapper.book(books.save(b));
    }

    public BookView updateBook(Long id, BookRequest r) {
        Book b = find(id);
        if (!b.getIsbn().equals(r.isbn().trim()) && books.existsByIsbn(r.isbn().trim())) {
            throw ApiException.conflict("ISBN already exists");
        }
        apply(b, r);
        return mapper.book(b);
    }

    public void deleteBook(Long id) {
        books.delete(find(id));
        books.flush();   // surface FK conflicts (book already ordered) as 409 here
    }

    public CategoryView createCategory(NameRequest r) {
        if (categories.existsByNameIgnoreCase(r.name().trim())) throw ApiException.conflict("Category already exists");
        Category c = new Category();
        c.setName(r.name().trim());
        c = categories.save(c);
        return new CategoryView(c.getId(), c.getName());
    }

    public PublisherView createPublisher(NameRequest r) {
        Publisher p = new Publisher();
        p.setName(r.name().trim());
        p = publishers.save(p);
        return new PublisherView(p.getId(), p.getName());
    }

    public AuthorView createAuthor(AuthorRequest r) {
        Author a = new Author();
        a.setName(r.name().trim());
        a.setBio(r.bio());
        a = authors.save(a);
        return new AuthorView(a.getId(), a.getName(), a.getBio());
    }

    private void apply(Book b, BookRequest r) {
        b.setTitle(r.title().trim());
        b.setDescription(r.description());
        b.setIsbn(r.isbn().trim());
        b.setAuthor(authors.findById(r.authorId()).orElseThrow(() -> ApiException.badRequest("authorId does not exist")));
        b.setPublisher(publishers.findById(r.publisherId()).orElseThrow(() -> ApiException.badRequest("publisherId does not exist")));
        b.setCategory(categories.findById(r.categoryId()).orElseThrow(() -> ApiException.badRequest("categoryId does not exist")));
        b.setFormat(r.format());
        if (r.language() != null && !r.language().isBlank()) b.setLanguage(r.language().trim());
        b.setPrice(r.price());
        b.setStock(r.stock());
        b.setCoverUrl(r.coverUrl());
        if (r.deliveryDays() != null) b.setDeliveryDays(r.deliveryDays());
    }

    private Book find(Long id) {
        return books.findById(id).orElseThrow(() -> ApiException.notFound("Book not found"));
    }
}
