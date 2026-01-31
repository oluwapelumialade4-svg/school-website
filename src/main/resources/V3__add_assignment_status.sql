-- Safely add status column to assignment table
ALTER TABLE assignment ADD COLUMN IF NOT EXISTS status VARCHAR(50);

-- Backfill existing records with a default value to prevent null issues
UPDATE assignment SET status = 'PUBLISHED' WHERE status IS NULL;