ALTER TABLE  entrant
    ADD is_scratched BOOLEAN NOT NULL DEFAULT FALSE,
    ADD scratched_time TIMESTAMP WITH TIME ZONE;
CREATE TABLE IF NOT EXISTS results (
      id bigserial PRIMARY KEY,
      entrant_id VARCHAR(255),
      market_id VARCHAR(36) NOT NULL,
      position INT NOT NULL,
      result_status_id VARCHAR(36) NOT NULL
    );
