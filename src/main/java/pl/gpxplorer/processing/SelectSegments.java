package pl.gpxplorer.processing;

import pl.gpxplorer.model.SegmentList;
import pl.gpxplorer.map.RouteLayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SelectSegments {
    private final List<RouteLayer> selectedLayers = new ArrayList<>();

    public void register(RouteLayer layer) {
        layer.setSelected(false);
        layer.setOnRouteClicked(this::toggleSelection);
    }

    public void clear() {
        for (RouteLayer layer : selectedLayers) {
            layer.setSelected(false);
        }
        selectedLayers.clear();
    }

    public SegmentList getSelectedSegments() {
        SegmentList segmentList = new SegmentList();
        for (RouteLayer layer : selectedLayers) {
            segmentList.add(layer.getRoute());
        }
        return segmentList;
    }

    public List<RouteLayer> getSelectedLayers() {
        List<RouteLayer> layers = new ArrayList<>(selectedLayers);
        layers.sort(Comparator.comparingInt(RouteLayer::getOrderIndex));
        return layers;
    }

    private void toggleSelection(RouteLayer layer) {
        if (layer.isSelected()) {
            layer.setSelected(false);
            selectedLayers.remove(layer);
        }else {
            layer.setSelected(true);
            selectedLayers.add(layer);
        }
    }
}
