CREATE TABLE IF NOT EXISTS meeting (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    advertised_date DATE NOT NULL,
    category_id VARCHAR(255) NOT NULL,
    venue_id VARCHAR(255) NOT NULL,
    track_condition VARCHAR(255) NOT NULL,
    country VARCHAR(255) NOT NULL,
    state VARCHAR(255) NOT NULL,
    has_fixed BOOLEAN NOT NULL,
    region_id VARCHAR(255) NOT NULL,
    feed_id VARCHAR(255) NOT NULL
    );