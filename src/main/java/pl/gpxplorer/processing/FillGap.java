package pl.gpxplorer.processing;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import pl.gpxplorer.model.Point;
import pl.gpxplorer.model.PointsList;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

public class FillGap {
    private static final String API_URL =
            "https://routing.openstreetmap.de/%s/route/v1/driving/%s,%s;%s,%s?overview=full&geometries=geojson";
    private static final double MAX_SNAP_METERS = 2000;

    public enum Profile {
        CAR("Samochod", "routed-car"),
        BIKE("Rower", "routed-bike"),
        FOOT("Pieszo", "routed-foot");

        private final String label;
        private final String server;

        Profile(String label, String server) {
            this.label = label;
            this.server = server;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public PointsList fill(Point from, Point to, Profile profile) throws IOException, InterruptedException {
        String url = String.format(API_URL, profile.server,
                format(from.lon()), format(from.lat()), format(to.lon()), format(to.lat()));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "GPXplorer/1.0")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject root = parse(response);

        String code = root.has("code") ? root.get("code").getAsString() : "";
        if (!"Ok".equals(code)) {
            if ("NoRoute".equals(code)) {
                throw new IllegalStateException("Nie znaleziono trasy (" + profile + ") miedzy koncem pierwszej a poczatkiem drugiej trasy.");
            }
            String message = root.has("message") ? root.get("message").getAsString() : code;
            throw new IllegalStateException("Serwer tras odrzucil zapytanie: " + message);
        }

        checkSnapDistances(root, profile);

        JsonArray routes = root.getAsJsonArray("routes");
        if (routes == null || routes.isEmpty()) {
            throw new IllegalStateException("Nie znaleziono trasy (" + profile + ").");
        }
        JsonArray coordinates = routes.get(0).getAsJsonObject()
                .getAsJsonObject("geometry")
                .getAsJsonArray("coordinates");

        PointsList pointsList = new PointsList();
        for (JsonElement element : coordinates) {
            JsonArray coord = element.getAsJsonArray();
            double lon = coord.get(0).getAsDouble();
            double lat = coord.get(1).getAsDouble();
            pointsList.add(new Point(lat, lon));
        }
        return pointsList;
    }

    private void checkSnapDistances(JsonObject root, Profile profile) {
        JsonArray waypoints = root.getAsJsonArray("waypoints");
        if (waypoints == null) {
            return;
        }
        String[] names = {"Koniec pierwszej trasy", "Poczatek drugiej trasy"};
        for (int i = 0; i < waypoints.size() && i < names.length; i++) {
            JsonObject waypoint = waypoints.get(i).getAsJsonObject();
            if (!waypoint.has("distance")) {
                continue;
            }
            double snapMeters = waypoint.get("distance").getAsDouble();
            if (snapMeters > MAX_SNAP_METERS) {
                throw new IllegalStateException(String.format(Locale.ROOT,
                        "%s jest %.1f km od najblizszej drogi dostepnej dla profilu \"%s\". Nie da sie wyznaczyc trasy laczacej.",
                        names[i], snapMeters / 1000.0, profile));
            }
        }
    }

    private JsonObject parse(HttpResponse<String> response) throws IOException {
        try {
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException("Serwer tras zwrocil nieprawidlowa odpowiedz (kod HTTP " + response.statusCode() + ").", e);
        }
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }
}
