package pl.gpxplorer.chart;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import pl.gpxplorer.processing.ElevationProfile;

import java.util.List;
import java.util.Locale;

public class ElevationChartWindow {
    private static final int MAX_CHART_POINTS = 1500;
    private static final String STAT_STYLE =
            "-fx-background-color: #243140; -fx-background-radius: 8; -fx-text-fill: #fff8ee; "
                    + "-fx-padding: 6 12; -fx-font-size: 13px; -fx-font-weight: 700;";

    public static void show(Window owner, ElevationProfile profile, String routeName) {
        FlowPane stats = new FlowPane(10, 10,
                stat("Dystans", "%.2f km", profile.getTotalDistanceKm()),
                stat("Podejscia", "%.0f m", profile.getAscent()),
                stat("Zejscia", "%.0f m", profile.getDescent()),
                stat("Min", "%.0f m", profile.getMinElevation()),
                stat("Max", "%.0f m", profile.getMaxElevation())
        );
        stats.setPadding(new Insets(12));

        TabPane tabs = new TabPane(
                new Tab("Profil wysokosci", elevationChart(profile)),
                new Tab("Nachylenie", slopeChart(profile))
        );
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        BorderPane root = new BorderPane(tabs);
        root.setTop(stats);
        root.setStyle("-fx-background-color: #fff8ee;");

        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.setTitle("Profil wysokosci - " + routeName);
        stage.setScene(new Scene(root, 960, 560));
        stage.show();
    }

    private static AreaChart<Number, Number> elevationChart(ElevationProfile profile) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Dystans [km]");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Wysokosc [m n.p.m.]");
        yAxis.setForceZeroInRange(false);

        AreaChart<Number, Number> chart = new AreaChart<>(xAxis, yAxis);
        chart.getData().add(series(profile.getElevationSamples()));
        chart.setCreateSymbols(false);
        styleChart(chart);
        return chart;
    }

    private static LineChart<Number, Number> slopeChart(ElevationProfile profile) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Dystans [km]");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Nachylenie [%]");

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.getData().add(series(profile.getSlopeSamples()));
        chart.setCreateSymbols(false);
        styleChart(chart);
        return chart;
    }

    private static XYChart.Series<Number, Number> series(List<double[]> samples) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        int step = Math.max(1, (int) Math.ceil(samples.size() / (double) MAX_CHART_POINTS));
        for (int i = 0; i < samples.size(); i += step) {
            double[] sample = samples.get(i);
            series.getData().add(new XYChart.Data<>(sample[0], sample[1]));
        }
        if (!samples.isEmpty() && (samples.size() - 1) % step != 0) {
            double[] last = samples.getLast();
            series.getData().add(new XYChart.Data<>(last[0], last[1]));
        }
        return series;
    }

    private static void styleChart(XYChart<Number, Number> chart) {
        chart.setLegendVisible(false);
        chart.setAnimated(false);
    }

    private static Label stat(String name, String format, double value) {
        Label label = new Label(name + ": " + String.format(Locale.ROOT, format, value));
        label.setStyle(STAT_STYLE);
        return label;
    }
}
