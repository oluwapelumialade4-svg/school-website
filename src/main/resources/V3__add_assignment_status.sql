-- V3__add_assignment_status.sql
ALTER TABLE assignment ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'PUBLISHED' NOT NULL;