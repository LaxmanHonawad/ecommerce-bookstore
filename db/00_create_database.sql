-- Run as a PostgreSQL superuser (e.g. postgres) connected to the default "postgres" database.
-- Creates the application role and the "bookstore" database. Safe to re-run.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'bookstore_user') THEN
        CREATE ROLE bookstore_user LOGIN PASSWORD 'bookstore_pass';
    END IF;
END
$$;

SELECT 'CREATE DATABASE bookstore OWNER bookstore_user ENCODING ''UTF8'''
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'bookstore')
\gexec
