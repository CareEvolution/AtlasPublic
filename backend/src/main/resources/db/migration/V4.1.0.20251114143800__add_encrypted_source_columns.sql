-- Add encrypted columns to source table
ALTER TABLE ${ohdsiSchema}.source
ALTER COLUMN source_connection TYPE TEXT,
ALTER COLUMN username TYPE TEXT,
ALTER COLUMN password TYPE TEXT;
