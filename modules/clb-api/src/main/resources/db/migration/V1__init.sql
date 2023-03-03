CREATE TABLE IF NOT EXISTS meeting (
    id bigserial PRIMARY KEY,
    meeting_id VARCHAR(255) UNIQUE,
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

CREATE TABLE IF NOT EXISTS race (
  id bigserial PRIMARY KEY,
  race_id VARCHAR(255) UNIQUE,
  meeting_id VARCHAR(255),
  name VARCHAR(255),
  number INTEGER,
  advertised_start TIMESTAMP WITH TIME ZONE,
  actual_start TIMESTAMP WITH TIME ZONE,
  market_ids JSONB,
  main_market_status_id VARCHAR(255),
  results_display VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS entrant (
      id bigserial PRIMARY KEY,
      entrant_id VARCHAR(255) UNIQUE ,
      race_id VARCHAR(255)
      name VARCHAR(255) NOT NULL,
      barrier INT NOT NULL,
      number INT NOT NULL,
      market_id VARCHAR(36) NOT NULL,
      visible BOOLEAN NOT NULL,
      price_fluctuations JSONB NOT NULL
);