ALTER TABLE race
ALTER COLUMN results_display TYPE JSONB USING results_display::JSONB;