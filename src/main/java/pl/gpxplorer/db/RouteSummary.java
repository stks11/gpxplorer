package pl.gpxplorer.db;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public record RouteSummary(
        long id,
        String name,
        OffsetDateTime createdAt,
        int segments,
        int points,
        double lengthKm
) {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public String toString() {
        String date = createdAt.atZoneSameInstant(ZoneId.systemDefault()).format(DATE_FORMAT);
        return String.format("%s  —  %.2f km, %d segm., %d pkt  (%s)", name, lengthKm, segments, points, date);
    }
}
