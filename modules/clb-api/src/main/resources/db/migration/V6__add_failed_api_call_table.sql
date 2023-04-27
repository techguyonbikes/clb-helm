CREATE TABLE IF NOT EXISTS failed_api_call (
    id bigserial PRIMARY KEY,
    class_name VARCHAR(255),
    method_name VARCHAR(255),
    params TEXT,
    failed_time TIMESTAMP WITH TIME ZONE
);