# Book Worm - E-Bookstore Backend (AI Specialist Capstone)

Spring Boot 3.3 + PostgreSQL REST API for browsing, buying and managing books, based on the capstone slides (member, store, catalog, order, payment, shipping).

## Prerequisites
- PostgreSQL 18 (installed at `C:\Program Files\PostgreSQL\18`, service `postgresql-x64-18`)
- JDK 17+ (JDK 20 is at `C:\Program Files\Java\jdk-20`)
- Maven (downloaded to `tools\apache-maven-3.9.9`, not committed)

## Run
```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-20'
$mvn = '.\tools\apache-maven-3.9.9\bin\mvn.cmd'

# 1. Create database, schema and seed data (prompts for the postgres superuser password)
.\db\setup-db.ps1

# 2. Build and start (http://localhost:8080)
& $mvn clean package -DskipTests
& $mvn spring-boot:run

# 3. In another terminal, verify every endpoint
.\scripts\smoke-test.ps1
```
Database settings are in `src/main/resources/application.properties` (`bookstore_user` / `bookstore_pass`, overridable with `DB_URL`, `DB_USER`, `DB_PASSWORD`).

## Contents
| Path | Purpose |
|---|---|
| `db/00_create_database.sql` | Creates role `bookstore_user` and database `bookstore` |
| `db/01_schema.sql` | 15 tables, constraints and indexes |
| `db/02_seed.sql` | Categories from the wireframe, 15 books, coupons, demo users, one delivered order |
| `db/setup-db.ps1` | Runs the three scripts in order |
| `docs/API.md` | Endpoint reference |
| `scripts/smoke-test.ps1` | Calls every endpoint (about 90 checks incl. error cases) |
| `src/main/java/com/bookworm` | `controller`, `service`, `repository`, `model`, `dto`, `security`, `exception`, `config` |

## Design notes
- Schema is owned by the SQL scripts (`ddl-auto=none`), so the DB script is the single source of truth.
- Auth is a simple opaque Bearer token stored in `auth_tokens` (bcrypt password hashes). It is enough for the capstone; use Spring Security/JWT for production.
- Payment gateway is simulated; no real card data is stored.
