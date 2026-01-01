CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE IF NOT EXISTS stations (
    id           UUID PRIMARY KEY,
    name         VARCHAR(255),
    brand        VARCHAR(255),
    latitude     DOUBLE PRECISION,
    longitude    DOUBLE PRECISION,
    post_code    VARCHAR(10),
    place        VARCHAR(255),
    street       VARCHAR(255),
    house_number VARCHAR(20)
    );

CREATE TABLE IF NOT EXISTS prices (
    time       TIMESTAMPTZ NOT NULL,
    station_id UUID NOT NULL,
    name       VARCHAR(128),
    price      NUMERIC(5,3),
    CONSTRAINT fk_station FOREIGN KEY(station_id) REFERENCES stations(id)
    );

SELECT create_hypertable('prices', 'time', if_not_exists => TRUE);

CREATE INDEX IF NOT EXISTS idx_price_station_time ON prices (station_id, name, time DESC);