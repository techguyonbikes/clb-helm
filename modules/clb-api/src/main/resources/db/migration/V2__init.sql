ALTER TABLE  entrant
    ADD is_scratched BOOLEAN NOT NULL DEFAULT FALSE,
    ADD scratched_time TIMESTAMP WITH TIME ZONE,
    ADD position INT NOT NULL;

ALTER TABLE  race
    ADD distance INT;
