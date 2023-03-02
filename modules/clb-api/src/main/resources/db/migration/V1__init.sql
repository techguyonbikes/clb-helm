CREATE TABLE IF NOT EXISTS meeting (
    id bigserial PRIMARY KEY,
    meeting_id VARCHAR(255),
    name VARCHAR(255),
    advertised_date TIMESTAMP WITH TIME ZONE,
    category_id VARCHAR(255),
    venue_id VARCHAR(255),
    track_condition VARCHAR(255),
    country VARCHAR(255),
    state VARCHAR(255),
    has_fixed BOOLEAN,
    region_id VARCHAR(255),
    feed_id VARCHAR(255),
    race_type VARCHAR(255)
);