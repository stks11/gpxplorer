package pl.gpxplorer.processing;

import pl.gpxplorer.model.Point;
import pl.gpxplorer.model.PointsList;
import pl.gpxplorer.map.RouteLayer;

import java.util.List;
import java.util.stream.Collectors;

public class MergeGPX {
    public PointsList mergeLayers(List<RouteLayer> layers) {
        List<Point> list = layers.stream()
                .sorted((left, right) -> Integer.compare(left.getOrderIndex(), right.getOrderIndex()))
                .flatMap(layer -> layer.getRoute().getPoints().stream())
                .collect(Collectors.toList());
        return toPointsList(list);
    }

    private PointsList toPointsList(List<Point> points) {
        PointsList pointsList = new PointsList();
        for (Point point : points) {
            pointsList.add(point);
        }
        return pointsList;
    }

}
