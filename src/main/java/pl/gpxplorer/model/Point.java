package pl.gpxplorer.model;

public record Point(
        double lat,
        double lon
) {
    @Override
    public String toString() {
        return lat + " " + lon;
    }
}
