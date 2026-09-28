package pl.gpxplorer.db;

import pl.gpxplorer.model.Point;
import pl.gpxplorer.model.PointsList;
import pl.gpxplorer.model.SegmentList;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class RouteRepository {
    private static final String INSERT_SQL = """
            INSERT INTO routes (name, geom)
            VALUES (?, ST_GeomFromText(?, 4326))
            RETURNING id
            """;

    private static final String LIST_SQL = """
            SELECT id, name, created_at,
                   ST_NumGeometries(geom) AS segments,
                   ST_NPoints(geom) AS points,
                   ST_Length(geom::geography) / 1000.0 AS length_km
            FROM routes
            ORDER BY created_at DESC, id DESC
            """;

    private static final String LOAD_SQL = """
            SELECT (dp.path)[1] AS segment_no, ST_Y(dp.geom) AS lat, ST_X(dp.geom) AS lon, ST_Z(dp.geom) AS ele
            FROM routes r, ST_DumpPoints(r.geom) AS dp
            WHERE r.id = ?
            ORDER BY dp.path
            """;

    private static final String DELETE_SQL = "DELETE FROM routes WHERE id = ?";

    private final DatabaseConfig config;

    public RouteRepository(DatabaseConfig config) {
        this.config = config;
    }

    public long save(String name, SegmentList segments) throws SQLException {
        String wkt = toMultiLineStringWkt(segments);
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, name);
            statement.setString(2, wkt);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    public List<RouteSummary> listRoutes() throws SQLException {
        List<RouteSummary> routes = new ArrayList<>();
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(LIST_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                routes.add(new RouteSummary(
                        resultSet.getLong("id"),
                        resultSet.getString("name"),
                        resultSet.getObject("created_at", OffsetDateTime.class),
                        resultSet.getInt("segments"),
                        resultSet.getInt("points"),
                        resultSet.getDouble("length_km")
                ));
            }
        }
        return routes;
    }

    public SegmentList load(long id) throws SQLException {
        SegmentList segmentList = new SegmentList();
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(LOAD_SQL)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                int currentSegment = -1;
                PointsList segment = null;
                while (resultSet.next()) {
                    int segmentNo = resultSet.getInt("segment_no");
                    if (segmentNo != currentSegment) {
                        segment = new PointsList();
                        segmentList.add(segment);
                        currentSegment = segmentNo;
                    }
                    double ele = resultSet.getDouble("ele");
                    if (resultSet.wasNull()) {
                        ele = Double.NaN;
                    }
                    segment.add(new Point(resultSet.getDouble("lat"), resultSet.getDouble("lon"), ele));
                }
            }
        }
        return segmentList;
    }

    public void delete(long id) throws SQLException {
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(config.url(), config.user(), config.password());
    }

    private String toMultiLineStringWkt(SegmentList segments) {
        List<PointsList> savable = new ArrayList<>();
        for (PointsList segment : segments) {
            if (segment.getLength() >= 2) {
                savable.add(segment);
            }
        }
        if (savable.isEmpty()) {
            throw new IllegalArgumentException("Trasa musi miec co najmniej jeden segment z dwoma punktami.");
        }
        boolean withElevation = savable.stream()
                .allMatch(segment -> segment.getPoints().stream().allMatch(Point::hasElevation));

        List<String> lines = new ArrayList<>();
        for (PointsList segment : savable) {
            List<String> coordinates = new ArrayList<>();
            for (Point point : segment) {
                String coordinate = format(point.lon()) + " " + format(point.lat());
                if (withElevation) {
                    coordinate += " " + format(point.ele());
                }
                coordinates.add(coordinate);
            }
            lines.add("(" + String.join(",", coordinates) + ")");
        }
        String type = withElevation ? "MULTILINESTRING Z" : "MULTILINESTRING";
        return type + "(" + String.join(",", lines) + ")";
    }

    private String format(double value) {
        return BigDecimal.valueOf(value).toPlainString();
    }
}
