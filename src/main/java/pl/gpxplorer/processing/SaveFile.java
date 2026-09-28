package pl.gpxplorer.processing;

import pl.gpxplorer.model.Point;
import pl.gpxplorer.model.PointsList;
import pl.gpxplorer.model.SegmentList;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SaveFile {
    private static final String GPX_HEADER = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="GPXplorer" xmlns="http://www.topografix.com/GPX/1/1">
              <trk>
                <name>GPXplorer Export</name>
            """;

    private static final String GPX_FOOTER = """
              </trk>
            </gpx>
            """;

    public void save(PointsList pointsList, File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(GPX_HEADER);
            writeSegment(writer, pointsList);
            writer.write(GPX_FOOTER);
        } catch (IOException exception) {
            throw new RuntimeException("Nie udalo sie zapisac pliku GPX.", exception);
        }
    }

    public void save(SegmentList segmentList, File file) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(GPX_HEADER);
            for (PointsList segment : segmentList) {
                writeSegment(writer, segment);
            }
            writer.write(GPX_FOOTER);
        } catch (IOException e) {
            throw new RuntimeException("Nie udalo sie zapisac pliku GPX.", e);
        }
    }

    private void writeSegment(FileWriter writer, PointsList pointsList) {
        try {
            writer.write("    <trkseg>\n");
            for (Point point : pointsList) {
                if (point.hasElevation()) {
                    writer.write(String.format(
                            "      <trkpt lat=\"%s\" lon=\"%s\"><ele>%s</ele></trkpt>%n",
                            point.lat(), point.lon(), point.ele()));
                } else {
                    writer.write(String.format(
                            "      <trkpt lat=\"%s\" lon=\"%s\"/>%n",
                            point.lat(), point.lon()));
                }
            }
            writer.write("    </trkseg>\n");
        } catch (IOException e) {
            throw new RuntimeException("Nie udało się zapisac pliku GPX. ", e);
        }

    }
}
