-- Book Worm e-bookstore schema (PostgreSQL 14+).
-- Run as bookstore_user connected to the "bookstore" database.
-- WARNING: drops and recreates all tables.

DROP TABLE IF EXISTS shipments, payments, order_items, orders, coupons, cart_items,
    wishlist_items, reviews, books, publishers, authors, categories, addresses,
    auth_tokens, users CASCADE;

-- ---------------------------------------------------------------- Member
CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER' CHECK (role IN ('CUSTOMER', 'ADMIN')),
    gift_points   INTEGER      NOT NULL DEFAULT 0 CHECK (gift_points >= 0),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_users_email ON users (lower(email));

CREATE TABLE auth_tokens (
    token      VARCHAR(64) PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_auth_tokens_user ON auth_tokens (user_id);

CREATE TABLE addresses (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    first_name VARCHAR(60)  NOT NULL,
    last_name  VARCHAR(60)  NOT NULL,
    email      VARCHAR(150),
    phone      VARCHAR(20)  NOT NULL,
    line1      VARCHAR(200) NOT NULL,
    line2      VARCHAR(200),
    city       VARCHAR(80)  NOT NULL,
    pin        VARCHAR(10)  NOT NULL,
    state      VARCHAR(80)  NOT NULL,
    country    VARCHAR(80)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_addresses_user ON addresses (user_id);

-- ---------------------------------------------------------------- Store / Catalog
CREATE TABLE categories (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE authors (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    bio  VARCHAR(1000)
);

CREATE TABLE publishers (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE
);

CREATE TABLE books (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title         VARCHAR(200)  NOT NULL,
    description   VARCHAR(2000),
    isbn          VARCHAR(20)   NOT NULL UNIQUE,
    author_id     BIGINT        NOT NULL REFERENCES authors (id),
    publisher_id  BIGINT        NOT NULL REFERENCES publishers (id),
    category_id   BIGINT        NOT NULL REFERENCES categories (id),
    format        VARCHAR(20)   NOT NULL CHECK (format IN ('Paperback', 'Hardcover', 'eBook')),
    language      VARCHAR(30)   NOT NULL DEFAULT 'English',
    price         NUMERIC(10,2) NOT NULL CHECK (price > 0),
    stock         INTEGER       NOT NULL DEFAULT 0 CHECK (stock >= 0),
    rating_avg    NUMERIC(3,2)  NOT NULL DEFAULT 0,
    rating_count  INTEGER       NOT NULL DEFAULT 0,
    sold_count    INTEGER       NOT NULL DEFAULT 0,
    cover_url     VARCHAR(300),
    published_on  DATE          NOT NULL DEFAULT current_date,
    delivery_days INTEGER       NOT NULL DEFAULT 3 CHECK (delivery_days >= 0),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX ix_books_category  ON books (category_id);
CREATE INDEX ix_books_author    ON books (author_id);
CREATE INDEX ix_books_publisher ON books (publisher_id);
CREATE INDEX ix_books_title     ON books (lower(title));

CREATE TABLE reviews (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    book_id    BIGINT      NOT NULL REFERENCES books (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    rating     INTEGER     NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment    VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (book_id, user_id)
);

CREATE TABLE wishlist_items (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    book_id    BIGINT      NOT NULL REFERENCES books (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, book_id)
);

-- ---------------------------------------------------------------- Cart / Order
CREATE TABLE cart_items (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    book_id    BIGINT      NOT NULL REFERENCES books (id) ON DELETE CASCADE,
    quantity   INTEGER     NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, book_id)
);

CREATE TABLE coupons (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code             VARCHAR(30)   NOT NULL,
    discount_type    VARCHAR(10)   NOT NULL CHECK (discount_type IN ('PERCENT', 'FLAT')),
    discount_value   NUMERIC(10,2) NOT NULL CHECK (discount_value > 0),
    min_order_amount NUMERIC(10,2) NOT NULL DEFAULT 0,
    active           BOOLEAN       NOT NULL DEFAULT TRUE,
    expires_at       TIMESTAMPTZ
);
CREATE UNIQUE INDEX ux_coupons_code ON coupons (upper(code));

CREATE TABLE orders (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id            BIGINT        NOT NULL REFERENCES users (id),
    status             VARCHAR(20)   NOT NULL
                       CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'RETURNED')),
    subtotal           NUMERIC(10,2) NOT NULL,
    tax                NUMERIC(10,2) NOT NULL,
    delivery_charge    NUMERIC(10,2) NOT NULL,
    coupon_code        VARCHAR(30),
    coupon_discount    NUMERIC(10,2) NOT NULL DEFAULT 0,
    gift_points_used   INTEGER       NOT NULL DEFAULT 0,
    gift_points_earned INTEGER       NOT NULL DEFAULT 0,
    total_amount       NUMERIC(10,2) NOT NULL,
    shipping_address   VARCHAR(600)  NOT NULL,
    estimated_delivery DATE,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    confirmed_at       TIMESTAMPTZ
);
CREATE INDEX ix_orders_user ON orders (user_id, created_at DESC);

CREATE TABLE order_items (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id   BIGINT        NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    book_id    BIGINT        NOT NULL REFERENCES books (id),
    title      VARCHAR(200)  NOT NULL,
    unit_price NUMERIC(10,2) NOT NULL,
    quantity   INTEGER       NOT NULL CHECK (quantity > 0)
);
CREATE INDEX ix_order_items_order ON order_items (order_id);

-- ---------------------------------------------------------------- Payment / Shipping
CREATE TABLE payments (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id        BIGINT        NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    method          VARCHAR(20)   NOT NULL CHECK (method IN ('CREDIT_CARD', 'DEBIT_CARD', 'UPI', 'WALLET')),
    amount          NUMERIC(10,2) NOT NULL,
    status          VARCHAR(10)   NOT NULL CHECK (status IN ('SUCCESS', 'FAILED', 'REFUNDED')),
    transaction_ref VARCHAR(60),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX ix_payments_order ON payments (order_id);

CREATE TABLE shipments (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id           BIGINT      NOT NULL UNIQUE REFERENCES orders (id) ON DELETE CASCADE,
    status             VARCHAR(20) NOT NULL
                       CHECK (status IN ('PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'RETURN_INITIATED')),
    tracking_number    VARCHAR(40),
    estimated_delivery DATE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
