# gpxplorer
A JavaFX desktop app for loading, splitting, merging, connecting, and saving GPX route segments on an interactive map.

## Database (PostGIS)
Routes can be saved to and loaded from a PostgreSQL + PostGIS database
(table `routes`, geometry `MultiLineString` in SRID 4326, one LineString per segment).

Start the database (requires Docker):

```
docker compose up -d
```

It listens on `localhost:5433` (db/user/password: `gpxplorer`) and creates the schema from `db/init/01-schema.sql` on first start.
Connection settings live in `src/main/resources/pl/gpxplorer/db.properties` and can be overridden with
`GPXPLORER_DB_URL`, `GPXPLORER_DB_USER` and `GPXPLORER_DB_PASSWORD`.
