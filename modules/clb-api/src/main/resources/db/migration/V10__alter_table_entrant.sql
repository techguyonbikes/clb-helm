ALTER TABLE entrant
    ADD COLUMN price_places JSONB NOT NULL
    ADD COLUMN barrier_position VARCHAR(255);
ALTER TABLE race
    ADD COLUMN venue_id VARCHAR(255);