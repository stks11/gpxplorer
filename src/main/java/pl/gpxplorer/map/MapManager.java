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
