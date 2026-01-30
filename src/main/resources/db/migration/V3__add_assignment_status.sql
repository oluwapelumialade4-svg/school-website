-- V3__add_assignment_status.sql
-- Add status column to assignment table if it doesn't exist (for existing databases)
ALTER TABLE assignment ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'PUBLISHED';
-- Update any existing rows that have NULL status
UPDATE assignment SET status = 'PUBLISHED' WHERE status IS NULL;
-- Make the column NOT NULL
ALTER TABLE assignment ALTER COLUMN status SET NOT NULL;