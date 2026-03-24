module pl.gpxplorer {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires org.kordamp.ikonli.javafx;
    requires eu.hansolo.tilesfx;
    requires com.gluonhq.maps;
    requires io.github.makbn.jlmap.fx;
    requires io.github.makbn.jlmap.api;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires com.google.gson;

    opens pl.gpxplorer to javafx.fxml;
    exports pl.gpxplorer;
}
