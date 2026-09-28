CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS routes (
    id         BIGSERIAL PRIMARY KEY,
    name       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    geom       geometry    NOT NULL
               CONSTRAINT routes_geom_multilinestring CHECK (GeometryType(geom) = 'MULTILINESTRING')
               CONSTRAINT routes_geom_srid CHECK (ST_SRID(geom) = 4326)
);

CREATE INDEX IF NOT EXISTS routes_geom_idx ON routes USING GIST (geom);
