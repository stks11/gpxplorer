package pl.gpxplorer.processing;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import pl.gpxplorer.model.Point;
import pl.gpxplorer.model.PointsList;
import pl.gpxplorer.map.RouteLayer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Locale;

public class FillGap {
    public PointsList fill(List<RouteLayer> routeLayers) throws IOException, InterruptedException {
        RouteLayer firstLayer = routeLayers.get(0);
        RouteLayer secondLayer =  routeLayers.get(1);

        Point p1 = firstLayer.getRoute().getPoints().getLast();
        Point p2 = secondLayer.getRoute().getPoints().getFirst();

        String url = String.format(Locale.US,
                "https://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=geojson",
                p1.lon(), p1.lat(), p2.lon(), p2.lat()
        );
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        String cords = response.body();
        JsonObject root = new JsonParser().parse(cords).getAsJsonObject();

        JsonArray routes = root.get("routes").getAsJsonArray();
        JsonObject firstRoute = routes.get(0).getAsJsonObject();
        JsonObject geometry = firstRoute.get("geometry").getAsJsonObject();
        JsonArray coordinates = geometry.get("coordinates").getAsJsonArray();

        PointsList pointsList = new PointsList();

        for (int i = 0; i < coordinates.size(); i++) {
            JsonArray coord = coordinates.get(i).getAsJsonArray();
            double lon = coord.get(0).getAsDouble();
            double lat = coord.get(1).getAsDouble();
            pointsList.add(new Point(lat, lon));
        }
        return pointsList;
    }
}
