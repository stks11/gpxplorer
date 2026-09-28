package pl.gpxplorer.processing;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import pl.gpxplorer.model.Point;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ElevationService {
    private static final String API_URL = "https://api.open-meteo.com/v1/elevation?latitude=%s&longitude=%s";
    private static final int BATCH_SIZE = 100;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public double[] fetch(List<Point> points) throws IOException, InterruptedException {
        double[] result = new double[points.size()];
        for (int start = 0; start < points.size(); start += BATCH_SIZE) {
            List<Point> batch = points.subList(start, Math.min(start + BATCH_SIZE, points.size()));
            double[] elevations = fetchBatch(batch);
            System.arraycopy(elevations, 0, result, start, elevations.length);
        }
        return result;
    }

    private double[] fetchBatch(List<Point> batch) throws IOException, InterruptedException {
        String latitudes = batch.stream().map(point -> format(point.lat())).collect(Collectors.joining(","));
        String longitudes = batch.stream().map(point -> format(point.lon())).collect(Collectors.joining(","));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format(API_URL, latitudes, longitudes)))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Serwis wysokosci zwrocil kod " + response.statusCode() + ": " + response.body());
        }
        JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray elevations = root.getAsJsonArray("elevation");
        if (elevations == null || elevations.size() != batch.size()) {
            throw new IOException("Nieprawidlowa odpowiedz serwisu wysokosci.");
        }
        double[] result = new double[batch.size()];
        for (int i = 0; i < elevations.size(); i++) {
            JsonElement value = elevations.get(i);
            result[i] = value.isJsonNull() ? Double.NaN : value.getAsDouble();
        }
        return result;
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }
}
