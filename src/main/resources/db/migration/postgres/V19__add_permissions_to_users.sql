-- Add permissions column to users table
ALTER TABLE users ADD COLUMN permissions VARCHAR(1000) DEFAULT '*';
