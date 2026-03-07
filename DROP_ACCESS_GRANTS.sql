-- Quick fix: Drop the old access_grants table
-- Run this SQL directly in your PostgreSQL database before starting the application

DROP TABLE IF EXISTS access_grants CASCADE;

-- The application will recreate it correctly via Flyway migrations V10 and V11
