ALTER TABLE race
ALTER COLUMN meeting_id TYPE BIGINT USING meeting_id::bigint;

ALTER TABLE entrant
ALTER COLUMN race_id TYPE BIGINT USING race_id::bigint;