
CREATE TABLE IF NOT EXISTS additionalinfo (
      id bigserial PRIMARY KEY,
      race_id VARCHAR(255),
      distance INTEGER,
      race_comment VARCHAR(1000),
      distance_type VARCHAR(255),
      generated INTEGER,
      silk_base_url VARCHAR(255),
      track_condition VARCHAR(255),
      weather VARCHAR(255)
    );
