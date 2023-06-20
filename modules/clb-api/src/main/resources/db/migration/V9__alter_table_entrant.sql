ALTER TABLE entrant
    ADD COLUMN last_6_starts VARCHAR(255),
    ADD COLUMN best_time VARCHAR(50),
    ADD COLUMN best_mile_rate VARCHAR(50),
    ADD COLUMN handicap_weight float,
    ADD COLUMN entrant_comment VARCHAR(1000);
ALTER TABLE race
    ADD COLUMN silk_url VARCHAR(255),
    ADD COLUMN full_form_url VARCHAR(255);