package pl.gpxplorer.model;

public record Point(
        double lat,
        double lon,
        double ele
) {
    public Point(double lat, double lon) {
        this(lat, lon, Double.NaN);
    }

    public boolean hasElevation() {
        return !Double.isNaN(ele);
    }

    @Override
    public String toString() {
        return lat + " " + lon;
    }
}
