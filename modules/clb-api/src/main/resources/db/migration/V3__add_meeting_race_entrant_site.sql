DROP TABLE IF EXISTS meeting_site;
CREATE TABLE meeting_site (
                              id bigserial PRIMARY KEY,
                              general_meeting_id bigint NOT NULL,
                              meeting_site_id VARCHAR NOT NULL,
                              site_id int NOT NULL,
                              start_date TIMESTAMP WITH TIME ZONE
                          );
CREATE TABLE race_site (
                           id bigserial PRIMARY KEY,
                           general_race_id bigint NOT NULL,
                           race_site_id VARCHAR NOT NULL,
                           site_id int NOT NULL,
                           start_date TIMESTAMP WITH TIME ZONE
);
CREATE TABLE entrant_site (
                              id bigserial PRIMARY KEY,
                              general_entrant_id bigint NOT NULL,
                              entrant_site_id VARCHAR NOT NULL,
                              site_id int NOT NULL,
                              price_fluctuations JSONB NOT NULL
);

ALTER TABLE meeting DROP COLUMN meeting_id;
ALTER TABLE race DROP COLUMN race_id;
ALTER TABLE entrant DROP COLUMN entrant_id;