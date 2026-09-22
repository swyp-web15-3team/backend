CREATE TABLE exchange_rate_snapshots (
    rate_date DATE PRIMARY KEY,
    rates JSONB NOT NULL
);
