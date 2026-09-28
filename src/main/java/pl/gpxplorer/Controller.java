package pl.gpxplorer;

import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import pl.gpxplorer.db.DatabaseConfig;
import pl.gpxplorer.db.RouteRepository;
import pl.gpxplorer.db.RouteSummary;
import pl.gpxplorer.map.MapManager;
import pl.gpxplorer.map.RouteLayer;
import pl.gpxplorer.model.PointsList;
import pl.gpxplorer.model.SegmentList;
import pl.gpxplorer.parsing.Parser;
import pl.gpxplorer.chart.ElevationChartWindow;
import pl.gpxplorer.model.Point;
import pl.gpxplorer.processing.ElevationProfile;
import pl.gpxplorer.processing.ElevationService;
import pl.gpxplorer.processing.FillGap;
import pl.gpxplorer.processing.MergeGPX;
import pl.gpxplorer.processing.SaveFile;
import pl.gpxplorer.processing.SplitGPXByKilometer;
import pl.gpxplorer.processing.SplitGPXByParts;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class Controller {
    private static final String DB_HINT = "\n\nCzy baza dziala? Uruchom ja poleceniem: docker compose up -d";
    private static final String NETWORK_HINT = "\n\nSprawdz polaczenie z internetem.";

    @FXML
    private Button fill;
    @FXML
    private StackPane mapContainer;

    private final MapView mapView = new MapView();

    private MapManager mapManager;
    private final RouteRepository routeRepository = new RouteRepository(DatabaseConfig.load());
    private final ElevationService elevationService = new ElevationService();
    private String lastRouteName;
    private final FillGap fillGap = new FillGap();
    private FillGap.Profile lastFillProfile = FillGap.Profile.CAR;
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
    private Button savedb;

    @FXML
    private Button loaddb;

    @FXML
    private Button elevation;



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
        setHoverCursor(savedb);
        setHoverCursor(loaddb);
        setHoverCursor(elevation);
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
        SegmentList selected = mapManager.getSelectedSegments();
        if (selected.getSize() == 0) {
            showError("Zaznacz najpierw segmenty do podzialu.");
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
        for (PointsList segment : selected) {
            if (parts > segment.getLength()) {
                showError("Liczba czesci nie moze byc wieksza niz liczba punktow segmentu.");
                return;
            }
        }
        SplitGPXByParts splitter = new SplitGPXByParts();
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
        PointsList route = parser.FileParser(file);
        System.out.println("loaded points = " + route.getLength());
        if (route.isEmpty()) {
            showError("Plik nie zawiera zadnych punktow trasy.");
            return;
        }
        System.out.println("first loaded point = " + route.getPoints().getFirst());
        lastRouteName = file.getName().replaceFirst("(?i)\\.gpx$", "");
        mapManager.showRoute(route);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Blad");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("GPXplorer");
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
        } else {
            mapManager.clearSelected();
        }
    }
    @FXML
    private void onSave() {
        SegmentList allSegments = mapManager.getAllSegments();
        if (allSegments.getSize() == 0) {
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
        System.out.println("saving segments = " + allSegments.getSize());
        saveFile.save(allSegments, file);
    }

    @FXML
    private void onSaveToDb() {
        SegmentList allSegments = mapManager.getAllSegments();
        if (allSegments.getSize() == 0) {
            showError("Na mapie nie ma zadnej trasy do zapisania.");
            return;
        }
        SegmentList selectedSegments = mapManager.getSelectedSegments();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Zapisz do bazy");
        dialog.setHeaderText("Podaj nazwe trasy");
        TextField nameField = new TextField(defaultRouteName());
        CheckBox onlySelectedCheckBox = new CheckBox("Zapisz tylko zaznaczone");
        onlySelectedCheckBox.setSelected(selectedSegments.getSize() > 0);
        onlySelectedCheckBox.setDisable(selectedSegments.getSize() == 0);
        VBox content = new VBox(new Label("Nazwa:"), nameField, onlySelectedCheckBox);
        content.setSpacing(10);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Node okButton = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.disableProperty().bind(nameField.textProperty().isEmpty());

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        String name = nameField.getText().trim();
        SegmentList segmentsToSave = onlySelectedCheckBox.isSelected() ? selectedSegments : allSegments;
        runInBackground(
                () -> routeRepository.save(name, segmentsToSave),
                id -> showInfo("Zapisano trase \"" + name + "\" w bazie (id " + id + ")."),
                "Nie udalo sie zapisac trasy w bazie."
        );
    }

    @FXML
    private void onLoadFromDb() {
        runInBackground(
                routeRepository::listRoutes,
                this::showRoutePicker,
                "Nie udalo sie pobrac listy tras z bazy."
        );
    }

    private void showRoutePicker(List<RouteSummary> routes) {
        if (routes.isEmpty()) {
            showInfo("W bazie nie ma jeszcze zadnych tras.");
            return;
        }
        ListView<RouteSummary> listView = new ListView<>();
        listView.getItems().setAll(routes);
        listView.setPrefSize(560, 320);

        Button deleteButton = new Button("Usun zaznaczona trase");
        deleteButton.disableProperty().bind(listView.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.setOnAction(e -> deleteRoute(listView));

        Dialog<RouteSummary> dialog = new Dialog<>();
        dialog.setTitle("Wczytaj z bazy");
        dialog.setHeaderText("Wybierz trase do wczytania");
        VBox content = new VBox(listView, deleteButton);
        content.setSpacing(10);
        dialog.getDialogPane().setContent(content);
        ButtonType loadButtonType = new ButtonType("Wczytaj", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loadButtonType, ButtonType.CANCEL);
        Button loadButton = (Button) dialog.getDialogPane().lookupButton(loadButtonType);
        loadButton.disableProperty().bind(listView.getSelectionModel().selectedItemProperty().isNull());
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !loadButton.isDisabled()) {
                loadButton.fire();
            }
        });
        dialog.setResultConverter(buttonType ->
                buttonType == loadButtonType ? listView.getSelectionModel().getSelectedItem() : null);

        dialog.showAndWait().ifPresent(route -> runInBackground(
                () -> routeRepository.load(route.id()),
                segments -> {
                    if (segments.getSize() == 0) {
                        showError("Trasa \"" + route.name() + "\" nie istnieje juz w bazie.");
                        return;
                    }
                    lastRouteName = route.name();
                    mapManager.showSegments(segments);
                    mapManager.zoomToSegments(segments);
                },
                "Nie udalo sie wczytac trasy z bazy."
        ));
    }

    private void deleteRoute(ListView<RouteSummary> listView) {
        RouteSummary route = listView.getSelectionModel().getSelectedItem();
        if (route == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Usun trase");
        confirm.setHeaderText(null);
        confirm.setContentText("Usunac trase \"" + route.name() + "\" z bazy?");
        Optional<ButtonType> answer = confirm.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.OK) {
            return;
        }
        runInBackground(
                () -> {
                    routeRepository.delete(route.id());
                    return route;
                },
                deleted -> listView.getItems().remove(deleted),
                "Nie udalo sie usunac trasy z bazy."
        );
    }

    private String defaultRouteName() {
        if (lastRouteName != null && !lastRouteName.isBlank()) {
            return lastRouteName;
        }
        return "Trasa " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private <T> void runInBackground(Callable<T> work, Consumer<T> onSuccess, String errorMessage) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(e -> {
            mapContainer.getScene().setCursor(Cursor.DEFAULT);
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(e -> {
            mapContainer.getScene().setCursor(Cursor.DEFAULT);
            Throwable exception = task.getException();
            exception.printStackTrace();
            String hint = "";
            if (exception instanceof SQLException) {
                hint = DB_HINT;
            } else if (exception instanceof IOException) {
                hint = NETWORK_HINT;
            }
            String details = exception.getMessage() != null ? exception.getMessage() : exception.getClass().getSimpleName();
            showError(errorMessage + "\n" + details + hint);
        });
        mapContainer.getScene().setCursor(Cursor.WAIT);
        Thread thread = new Thread(task, "gpxplorer-background");
        thread.setDaemon(true);
        thread.start();
    }

    private void setHoverCursor(Button button) {
        button.setOnMouseEntered(e -> button.setCursor(Cursor.HAND));
        button.setOnMouseExited(e -> button.setCursor(Cursor.DEFAULT));
    }
    @FXML
    private void onElevation() {
        SegmentList selected = mapManager.getSelectedSegments();
        SegmentList segments = selected.getSize() > 0 ? selected : mapManager.getAllSegments();
        if (segments.getSize() == 0) {
            showError("Najpierw wczytaj trase GPX.");
            return;
        }

        List<Point> missing = new ArrayList<>();
        int total = 0;
        for (PointsList segment : segments) {
            for (Point point : segment) {
                total++;
                if (!point.hasElevation()) {
                    missing.add(point);
                }
            }
        }
        if (missing.isEmpty()) {
            showElevationChart(segments);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Brak wysokosci");
        confirm.setHeaderText(null);
        confirm.setContentText(missing.size() + " z " + total + " punktow trasy nie ma danych o wysokosci.\n"
                + "Pobrac brakujace wysokosci z internetu (Open-Meteo)?");
        Optional<ButtonType> answer = confirm.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.OK) {
            if (missing.size() < total) {
                showElevationChart(segments);
            }
            return;
        }
        runInBackground(
                () -> elevationService.fetch(missing),
                elevations -> {
                    applyElevations(segments, elevations);
                    showElevationChart(segments);
                },
                "Nie udalo sie pobrac wysokosci."
        );
    }

    private void applyElevations(SegmentList segments, double[] elevations) {
        int next = 0;
        for (PointsList segment : segments) {
            List<Point> points = segment.getPoints();
            for (int i = 0; i < points.size(); i++) {
                Point point = points.get(i);
                if (!point.hasElevation()) {
                    points.set(i, new Point(point.lat(), point.lon(), elevations[next++]));
                }
            }
        }
    }

    private void showElevationChart(SegmentList segments) {
        ElevationProfile profile = ElevationProfile.of(segments);
        if (profile.isEmpty()) {
            showError("Trasa nie ma danych o wysokosci.");
            return;
        }
        String routeName = lastRouteName != null ? lastRouteName : "trasa";
        ElevationChartWindow.show(mapContainer.getScene().getWindow(), profile, routeName);
    }

    @FXML
    private void onFill(){
        List<RouteLayer> selectedLayers = mapManager.getSelectedLayers();
        if (selectedLayers.size() != 2) {
            showError("Zaznacz dokladnie dwie trasy GPX.");
            return;
        }
        PointsList first = selectedLayers.get(0).getRoute();
        PointsList second = selectedLayers.get(1).getRoute();
        if (first.isEmpty() || second.isEmpty()) {
            showError("Zaznaczone trasy nie moga byc puste.");
            return;
        }

        ChoiceDialog<FillGap.Profile> dialog = new ChoiceDialog<>(lastFillProfile, FillGap.Profile.values());
        dialog.setTitle("Wypelnij przerwe");
        dialog.setHeaderText("Jak ma prowadzic trasa laczaca?");
        dialog.setContentText("Srodek transportu:");
        Optional<FillGap.Profile> profile = dialog.showAndWait();
        if (profile.isEmpty()) {
            return;
        }
        lastFillProfile = profile.get();

        Point from = first.getPoints().getLast();
        Point to = second.getPoints().getFirst();
        runInBackground(
                () -> fillGap.fill(from, to, profile.get()),
                mapManager::showRoute,
                "Nie udalo sie wyznaczyc trasy laczacej."
        );
    }

    public void onClearSelection() {
        List<RouteLayer> selectedLayers = mapManager.getSelectedLayers();
        if (!selectedLayers.isEmpty()) {
            mapManager.clearSelection();
        }
    }
}
