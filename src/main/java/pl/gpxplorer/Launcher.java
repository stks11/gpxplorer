package pl.gpxplorer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Launcher extends Application{
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/pl/gpxplorer/MainView.fxml")
        );
        Scene scene = new Scene(loader.load());
        stage.setTitle("GPXplorer");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }
}
