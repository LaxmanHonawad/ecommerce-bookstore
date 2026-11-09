-- Seed data for the Book Worm e-bookstore. Run after 01_schema.sql, on an empty schema.
-- Demo logins:  admin@bookworm.com / Admin@123   (ADMIN)
--               demo@bookworm.com  / Demo@123    (CUSTOMER, 500 gift points, has one delivered order)

CREATE EXTENSION IF NOT EXISTS pgcrypto;   -- bcrypt hashes compatible with Spring's BCryptPasswordEncoder

INSERT INTO users (name, email, password_hash, role, gift_points) VALUES
 ('Admin',         'admin@bookworm.com', crypt('Admin@123', gen_salt('bf', 10)), 'ADMIN',    0),
 ('Demo Customer', 'demo@bookworm.com',  crypt('Demo@123',  gen_salt('bf', 10)), 'CUSTOMER', 500);

INSERT INTO addresses (user_id, first_name, last_name, email, phone, line1, line2, city, pin, state, country)
SELECT id, 'Demo', 'Customer', 'demo@bookworm.com', '+919876543210', '12 MG Road', 'Near City Mall',
       'Bengaluru', '560001', 'Karnataka', 'India'
FROM users WHERE email = 'demo@bookworm.com';

INSERT INTO categories (name) VALUES
 ('Romance'), ('Mystery'), ('Science Fiction'), ('Fantasy'), ('Historical'), ('Biography'),
 ('Self-help'), ('Memoir'), ('Travel'), ('Cooking'), ('Children''s'), ('Young Adult'),
 ('Comics & Graphic Novels'), ('Poetry'), ('Drama'), ('Science'), ('Philosophy'), ('Religion'),
 ('Language Learning');

INSERT INTO publishers (name) VALUES
 ('ABC Publishers'), ('Horizon Press'), ('Lakeview Books'), ('Northstar Publishing'), ('Little Owl Books');

INSERT INTO authors (name, bio) VALUES
 ('Arjun Patel',    'Author and productivity writer focused on deep work and attention.'),
 ('Raj Patel',      'Educator who writes about learning methods and curiosity.'),
 ('James Wright',   'Writer of practical guides to achieving goals with clarity and confidence.'),
 ('James Adams',    'Thriller novelist known for atmospheric mysteries.'),
 ('Jessica Martin', 'Romance and historical fiction author.'),
 ('Laura Mitchell', 'Science fiction author and former space-systems engineer.'),
 ('Daniel Reed',    'Writer, minimalist, and productivity coach based in San Francisco. Author of The Joy of Minimalism and The Focus Reset.'),
 ('Clara Nelson',   'Author of chilling mysteries and epic fantasy.'),
 ('Emily Parker',   'Children''s author and food writer.');

INSERT INTO books (title, description, isbn, author_id, publisher_id, category_id, format, language,
                   price, stock, rating_avg, rating_count, sold_count, cover_url, published_on, delivery_days)
SELECT v.title, v.descr, v.isbn, a.id, p.id, c.id, v.format, v.lang,
       v.price, v.stock, v.rating, v.rcount, v.sold, v.cover, current_date - v.age, v.dd
FROM (VALUES
 ('The Art of Focus',      'Practical guide to mastering focus and boosting productivity every day.',          '9780000000001', 'Arjun Patel',    'ABC Publishers',       'Self-help',        'Paperback', 'English', 399.00,  50, 4.50, 120, 820, '/covers/the-art-of-focus.jpg',      200, 2),
 ('The Art of Learning',   'Master the mindset and methods for effective lifelong learning.',                  '9780000000002', 'Raj Patel',      'Horizon Press',        'Self-help',        'Paperback', 'English', 259.00,  40, 4.20,  80, 640, '/covers/the-art-of-learning.jpg',   180, 2),
 ('The Path to Success',   'A practical guide to achieving goals with clarity and confidence.',                '9780000000003', 'James Wright',   'ABC Publishers',       'Self-help',        'Paperback', 'English', 359.00,  35, 4.40,  95, 710, '/covers/the-path-to-success.jpg',   160, 3),
 ('The Midnight Hour',     'Haunting tale of a man''s journey and the shadows of a forgotten past.',          '9780000000004', 'James Adams',    'Lakeview Books',       'Mystery',          'Paperback', 'English', 299.00,  60, 4.60, 210, 930, '/covers/the-midnight-hour.jpg',     300, 2),
 ('Beneath the Stars',     'A heartwarming tale where two souls discover who they need.',                      '9780000000005', 'Jessica Martin', 'Northstar Publishing', 'Romance',          'Hardcover', 'English', 499.00,  25, 4.30, 150, 880, '/covers/beneath-the-stars.jpg',     250, 4),
 ('The Final Frontier',    'A mission to space secrets to change humanity forever.',                           '9780000000006', 'Laura Mitchell', 'Northstar Publishing', 'Science Fiction', 'Paperback', 'English', 359.00,  45, 4.50, 130, 760, '/covers/the-final-frontier.jpg',    220, 3),
 ('The Joy of Minimalism', 'Declutter your life to uncover peace, clarity, and joy.',                          '9780000000007', 'Daniel Reed',    'ABC Publishers',       'Self-help',        'Paperback', 'English', 149.00,  70, 5.00,  40, 145, '/covers/the-joy-of-minimalism.jpg', 5,   2),
 ('The Vanishing House',   'A chilling mystery unfolds within a house that disappears.',                       '9780000000008', 'Clara Nelson',   'Lakeview Books',       'Mystery',          'eBook',     'English',  99.00, 999, 4.00,  22,  60, '/covers/the-vanishing-house.jpg',   8,   0),
 ('The Lost Kitten',       'A heartwarming tale of courage, friendship, and feline adventure.',                '9780000000009', 'Emily Parker',   'Little Owl Books',     'Children''s',      'Hardcover', 'English', 339.00,  30, 4.80,  18,  35, '/covers/the-lost-kitten.jpg',       3,   3),
 ('Whispers of the Old Kingdom', 'An epic quest across a realm where old magic is waking.',                    '9780000000010', 'Clara Nelson',   'Lakeview Books',       'Fantasy',          'Hardcover', 'English', 449.00,  20, 4.40,  75, 410, '/covers/whispers-old-kingdom.jpg',  120, 4),
 ('Letters from the Past', 'A sweeping historical story told through letters across two wars.',                '9780000000011', 'Jessica Martin', 'Northstar Publishing', 'Historical',       'Paperback', 'English', 329.00,  28, 4.10,  52, 300, '/covers/letters-from-the-past.jpg', 140, 3),
 ('Everyday Cooking Made Simple', 'Fast, healthy home recipes with everyday ingredients.',                     '9780000000012', 'Emily Parker',   'Horizon Press',        'Cooking',          'Paperback', 'English', 279.00,  33, 4.30,  64, 355, '/covers/everyday-cooking.jpg',      90,  3),
 ('Hindi Vyakaran Saral',  'Simple, structured Hindi grammar for learners.',                                   '9780000000013', 'Raj Patel',      'Horizon Press',        'Language Learning','Paperback', 'Hindi',   189.00,  55, 4.20,  30, 210, '/covers/hindi-vyakaran.jpg',       200, 2),
 ('Quantum Curiosity',     'A friendly tour through the strangest ideas in modern physics.',                   '9780000000014', 'Laura Mitchell', 'Horizon Press',        'Science',          'Hardcover', 'English', 549.00,  18, 4.70,  88, 270, '/covers/quantum-curiosity.jpg',    60,  4),
 ('Wanderlust Diaries',    'Travel stories and practical tips from 30 countries.',                             '9780000000015', 'Arjun Patel',    'Little Owl Books',     'Travel',           'Paperback', 'English', 379.00,  22, 4.00,  41, 190, '/covers/wanderlust-diaries.jpg',   110, 3)
) AS v(title, descr, isbn, author, publisher, category, format, lang, price, stock, rating, rcount, sold, cover, age, dd)
JOIN authors    a ON a.name = v.author
JOIN publishers p ON p.name = v.publisher
JOIN categories c ON c.name = v.category;

INSERT INTO coupons (code, discount_type, discount_value, min_order_amount, active, expires_at) VALUES
 ('WELCOME100', 'FLAT',    100.00,  300.00, TRUE, NULL),
 ('BOOK10',     'PERCENT',  10.00,    0.00, TRUE, NULL),
 ('FESTIVE20',  'PERCENT',  20.00, 1000.00, TRUE, now() + interval '1 year'),
 ('EXPIRED50',  'FLAT',     50.00,    0.00, TRUE, now() - interval '1 day');

INSERT INTO reviews (book_id, user_id, rating, comment, created_at)
SELECT b.id, u.id, 5, 'Very practical and easy to read.', now() - interval '10 days'
FROM books b, users u
WHERE b.title = 'The Art of Focus' AND u.email = 'demo@bookworm.com';

-- One delivered order for the demo customer (drives "Buy it again" and recommendations).
WITH u AS (SELECT id FROM users WHERE email = 'demo@bookworm.com'),
o AS (
    INSERT INTO orders (user_id, status, subtotal, tax, delivery_charge, coupon_discount, gift_points_used,
                        gift_points_earned, total_amount, shipping_address, estimated_delivery, created_at, confirmed_at)
    SELECT u.id, 'DELIVERED', 698.00, 83.76, 0.00, 0.00, 0, 15, 781.76,
           'Demo Customer, 12 MG Road, Near City Mall, Bengaluru, Karnataka - 560001, India, Phone: +919876543210',
           current_date - 15, now() - interval '20 days', now() - interval '20 days'
    FROM u RETURNING id
),
oi AS (
    INSERT INTO order_items (order_id, book_id, title, unit_price, quantity)
    SELECT o.id, b.id, b.title, b.price, 1
    FROM o, books b WHERE b.title IN ('The Art of Focus', 'The Midnight Hour')
    RETURNING order_id
),
p AS (
    INSERT INTO payments (order_id, method, amount, status, transaction_ref, created_at)
    SELECT o.id, 'CREDIT_CARD', 781.76, 'SUCCESS', 'TXN-SEED-0001', now() - interval '20 days' FROM o
    RETURNING order_id
)
INSERT INTO shipments (order_id, status, tracking_number, estimated_delivery, created_at, updated_at)
SELECT o.id, 'DELIVERED', 'BW0000000001', current_date - 15, now() - interval '20 days', now() - interval '15 days' FROM o;
