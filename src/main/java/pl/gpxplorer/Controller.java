package pl.gpxplorer;

import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import pl.gpxplorer.map.MapManager;
import pl.gpxplorer.map.RouteLayer;
import pl.gpxplorer.model.PointsList;
import pl.gpxplorer.model.SegmentList;
import pl.gpxplorer.parsing.Parser;
import pl.gpxplorer.processing.FillGap;
import pl.gpxplorer.processing.MergeGPX;
import pl.gpxplorer.processing.SaveFile;
import pl.gpxplorer.processing.SplitGPXByKilometer;
import pl.gpxplorer.processing.SplitGPXByParts;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class Controller {
    @FXML
    private Button fill;
    @FXML
    private StackPane mapContainer;

    private final MapView mapView = new MapView();

    private MapManager mapManager;
    private PointsList currentRoute;
    @FXML
    private Button merge;

    @FXML
    private Button save;

    @FXML
    private Button splitkm;

    @FXML
    private Button splitlayer;

    @FXML
    private Button loadfile;

    @FXML
    private Button clear;

    @FXML
    private Button clearselection;



    @FXML
    public void initialize() {
        mapView.setCenter(new MapPoint(52.23, 21.01));
        mapView.setZoom(10);

        mapView.prefWidthProperty().bind(mapContainer.widthProperty());
        mapView.prefHeightProperty().bind(mapContainer.heightProperty());

        mapContainer.getChildren().add(mapView);
        mapManager = new MapManager(mapView);
        setHoverCursor(merge);
        setHoverCursor(splitkm);
        setHoverCursor(splitlayer);
        setHoverCursor(save);
        setHoverCursor(loadfile);
        setHoverCursor(clear);
        setHoverCursor(fill);
        setHoverCursor(clearselection);
    }

    @FXML
    private void onOpenGpx() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open GPX");

        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("GPX", "*.gpx")
        );

        File file = chooser.showOpenDialog(mapContainer.getScene().getWindow());
        if (file == null) {
            return;
        }

        loadRouteFromFile(file);
    }

    @FXML
    private void onSplitLayer() {
        if (currentRoute == null || currentRoute.isEmpty()) {
            showError("Najpierw wczytaj trase GPX.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Podziel trase");
        dialog.setHeaderText("Podaj liczbe czesci");
        dialog.setContentText("Liczba czesci:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }
        int parts;
        try {
            parts = Integer.parseInt(result.get().trim());
        } catch (NumberFormatException exception) {
            showError("Wpisz poprawna liczbe calkowita.");
            return;
        }
        if (parts <= 0) {
            showError("Liczba czesci musi byc wieksza od zera.");
            return;
        }
        if (parts > currentRoute.getLength()) {
            showError("Liczba czesci nie moze byc wieksza niz liczba punktow trasy.");
            return;
        }
        SplitGPXByParts splitter = new SplitGPXByParts();
        SegmentList selected = mapManager.getSelectedSegments();
        SegmentList segmentList = new SegmentList();
        for (PointsList segment : selected) {
            SegmentList part = splitter.split(segment, parts);
            for (PointsList list : part) {
                segmentList.add(list);
            }
        }
        mapManager.clearSelected();
        mapManager.showSegments(segmentList);
    }

    @FXML
    private void onSplitKm() {
        SegmentList selected = mapManager.getSelectedSegments();
        if (selected.getSize() == 0) {
            showError("Zaznacz najpierw segmenty do podzialu.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Podziel trase");
        dialog.setHeaderText("Podaj dlugosc odcinka w kilometrach");
        dialog.setContentText("Kilometry:");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }
        double kilometers;
        try {
            kilometers = Double.parseDouble(result.get().trim());
        } catch (NumberFormatException exception) {
            showError("Wpisz poprawna liczbe, np. 1.5");
            return;
        }
        if (kilometers <= 0) {
            showError("Dlugosc musi byc wieksza od zera.");
            return;
        }
        double meters = kilometers * 1000;
        SplitGPXByKilometer splitter = new SplitGPXByKilometer();
        SegmentList segmentList = new SegmentList();
        for (PointsList segment : selected) {
            SegmentList part = splitter.split(segment, meters);
            for (PointsList list : part) {
                segmentList.add(list);
            }
        }
        mapManager.clearSelected();
        mapManager.showSegments(segmentList);
    }

    private void loadRouteFromFile(File file) {
        Parser parser = new Parser();
        currentRoute = parser.FileParser(file);
        System.out.println("loaded points = " + currentRoute.getLength());
        System.out.println("first loaded point = " + currentRoute.getPoints().getFirst());
        mapManager.showRoute(currentRoute);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Blad");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void onMerge() {
        List<RouteLayer> selectedLayers = mapManager.getSelectedLayers();
        if (selectedLayers.isEmpty()) {
            showError("Zaznacz najpierw segmenty do polaczenia.");
            return;
        }
        MergeGPX merger = new MergeGPX();
        PointsList pointsList = merger.mergeLayers(selectedLayers);
        mapManager.clearSelected();
        mapManager.showRoute(pointsList);
    }
    @FXML
    private void onClear() {
        SegmentList selectedSegments = mapManager.getSelectedSegments();
        if (selectedSegments.getSize() == 0) {
            mapManager.clearRoutes();
            currentRoute = null;
        } else {
            mapManager.clearSelected();
        }
    }
    @FXML
    private void onSave() {
        if (currentRoute == null || currentRoute.isEmpty()) {
            showError("Najpierw wczytaj trase GPX.");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Zapisz GPX");
        dialog.setHeaderText("Wybierz zakres zapisu");
        CheckBox saveSelectedCheckBox = new CheckBox("Zapisz zaznaczone");
        VBox content = new VBox(saveSelectedCheckBox);
        content.setSpacing(10);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> saveOption = dialog.showAndWait();
        if (saveOption.isEmpty() || saveOption.get() != ButtonType.OK) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Zapisz GPX");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("GPX", "*.gpx"));
        File file = chooser.showSaveDialog(mapContainer.getScene().getWindow());
        if (file == null) {
            return;
        }
        SaveFile saveFile = new SaveFile();
        if (saveSelectedCheckBox.isSelected()) {
            SegmentList selectedSegments = mapManager.getSelectedSegments();
            if (selectedSegments.getSize() == 0) {
                showError("Zaznacz najpierw segmenty do zapisu.");
                return;
            }
            saveFile.save(selectedSegments, file);
            return;
        }
        System.out.println("saving points = " + currentRoute.getLength());
        System.out.println("first saved point = " + currentRoute.getPoints().getFirst());
        saveFile.save(currentRoute, file);
    }

    private void setHoverCursor(Button button) {
        button.setOnMouseEntered(e -> button.setCursor(Cursor.HAND));
        button.setOnMouseExited(e -> button.setCursor(Cursor.DEFAULT));
    }
    @FXML
    private void onFill(){
        List<RouteLayer> selectedLayers = mapManager.getSelectedLayers();
        if (selectedLayers.size() != 2) {
            showError("Zaznacz dwie trasy GPX.");
        }
        FillGap fillGap = new FillGap();
        try {
            PointsList points = fillGap.fill(selectedLayers);
            mapManager.showRoute(points);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void onClearSelection() {
        List<RouteLayer> selectedLayers = mapManager.getSelectedLayers();
        if (!selectedLayers.isEmpty()) {
            mapManager.clearSelection();
        }
    }
}
