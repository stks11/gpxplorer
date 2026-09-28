package pl.gpxplorer.map;

import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;
import pl.gpxplorer.processing.SelectSegments;
import pl.gpxplorer.model.Point;
import pl.gpxplorer.model.PointsList;
import pl.gpxplorer.model.SegmentList;

import java.util.ArrayList;
import java.util.List;

public class MapManager {
    private static final double TILE_SIZE = 256;
    private static final double FIT_PADDING = 0.08;
    private static final double MIN_FIT_ZOOM = 2;
    private static final double MAX_FIT_ZOOM = 18;

    private final MapView mapView;
    private final List<RouteLayer> routeLayers = new ArrayList<>();
    private final SelectSegments selectSegments = new SelectSegments();

    public MapManager(MapView mapView) {
        this.mapView = mapView;
    }

    public void showRoute(PointsList route) {
        RouteLayer layer = new RouteLayer(route);
        selectSegments.register(layer);
        routeLayers.add(layer);
        reindexLayers();
        mapView.addLayer(layer);
        centerOnRoute(route);
    }

    public void showSegments(SegmentList segments) {
        selectSegments.clear();
        for (PointsList segment : segments) {
            RouteLayer layer = new RouteLayer(segment);
            selectSegments.register(layer);
            routeLayers.add(layer);
            mapView.addLayer(layer);
        }
        reindexLayers();
        if (segments.getSegmentList().size() == 1) {
            centerOnRoute(segments.getSegmentList().getFirst());
        } else if (segments.getSegmentList().size()>1) {
            centerOnRoute(segments.getSegmentList().get(segments.getSize()/2));
        }
    }

    public SegmentList getSelectedSegments() {
        return selectSegments.getSelectedSegments();
    }

    public SegmentList getAllSegments() {
        SegmentList segmentList = new SegmentList();
        for (RouteLayer layer : routeLayers) {
            segmentList.add(layer.getRoute());
        }
        return segmentList;
    }

    public List<RouteLayer> getSelectedLayers() {
        return selectSegments.getSelectedLayers();
    }

    public void clearRoutes() {
        for (RouteLayer layer : routeLayers) {
            mapView.removeLayer(layer);
        }
        routeLayers.clear();
        selectSegments.clear();
    }

    public void clearSelection() {
        selectSegments.clear();
    }

    public void clearSelected(){
        List<RouteLayer> layersToRemove = new ArrayList<>();
        for (RouteLayer layer : routeLayers) {
            if (layer.isSelected()) {
                layersToRemove.add(layer);
            }
        }

        for (RouteLayer layer : layersToRemove) {
            System.out.println("removing layer orderIndex=" + layer.getOrderIndex());
            mapView.removeLayer(layer);
            routeLayers.remove(layer);
        }
        selectSegments.clear();
        reindexLayers();
    }

    public void zoomToSegments(SegmentList segments) {
        double minLat = Double.POSITIVE_INFINITY;
        double maxLat = Double.NEGATIVE_INFINITY;
        double minLon = Double.POSITIVE_INFINITY;
        double maxLon = Double.NEGATIVE_INFINITY;
        for (PointsList segment : segments) {
            for (Point point : segment) {
                minLat = Math.min(minLat, point.lat());
                maxLat = Math.max(maxLat, point.lat());
                minLon = Math.min(minLon, point.lon());
                maxLon = Math.max(maxLon, point.lon());
            }
        }
        if (minLat == Double.POSITIVE_INFINITY) {
            return;
        }

        double width = mapView.getWidth() > 0 ? mapView.getWidth() : 800;
        double height = mapView.getHeight() > 0 ? mapView.getHeight() : 600;
        double usableWidth = width * (1 - 2 * FIT_PADDING);
        double usableHeight = height * (1 - 2 * FIT_PADDING);

        double lonFraction = (maxLon - minLon) / 360.0;
        double latFraction = (mercatorY(maxLat) - mercatorY(minLat)) / (2 * Math.PI);
        double zoomLon = lonFraction > 0 ? log2(usableWidth / (TILE_SIZE * lonFraction)) : MAX_FIT_ZOOM;
        double zoomLat = latFraction > 0 ? log2(usableHeight / (TILE_SIZE * latFraction)) : MAX_FIT_ZOOM;
        double zoom = Math.floor(Math.min(zoomLon, zoomLat));
        zoom = Math.max(MIN_FIT_ZOOM, Math.min(MAX_FIT_ZOOM, zoom));

        double centerLat = inverseMercatorY((mercatorY(minLat) + mercatorY(maxLat)) / 2);
        double centerLon = (minLon + maxLon) / 2;

        mapView.setZoom(zoom);
        mapView.setCenter(new MapPoint(centerLat, centerLon));
        for (RouteLayer layer : routeLayers) {
            layer.refresh();
        }
    }

    private static double mercatorY(double lat) {
        double radians = Math.toRadians(lat);
        return Math.log(Math.tan(Math.PI / 4 + radians / 2));
    }

    private static double inverseMercatorY(double y) {
        return Math.toDegrees(2 * Math.atan(Math.exp(y)) - Math.PI / 2);
    }

    private static double log2(double value) {
        return Math.log(value) / Math.log(2);
    }

    private void centerOnRoute(PointsList route) {
        if (route == null || route.isEmpty()) {
            return;
        }
        Point point = route.getPoints().getFirst();
        mapView.setCenter(new MapPoint(point.lat(), point.lon()));
    }

    private void reindexLayers() {
        for (int index = 0; index < routeLayers.size(); index++) {
            routeLayers.get(index).setOrderIndex(index);
            System.out.println("layer " + index + " -> orderIndex=" + routeLayers.get(index).getOrderIndex());
        }
    }
}
