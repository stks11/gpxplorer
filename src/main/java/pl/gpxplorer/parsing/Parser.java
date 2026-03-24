package pl.gpxplorer.parsing;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import pl.gpxplorer.model.Point;
import pl.gpxplorer.model.PointsList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class Parser {
    public PointsList FileParser(File file){
        PointsList points = new PointsList();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        try {
            db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);

            NodeList list = doc.getElementsByTagName("trkpt");
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
//                System.out.println(node.getAttributes().getNamedItem("lat") + " " + node.getAttributes().getNamedItem("lon"));
                String latStr = node.getAttributes().getNamedItem("lat").getTextContent();
                String lonStr = node.getAttributes().getNamedItem("lon").getTextContent();
                double lat = Double.parseDouble(latStr);
                double lon = Double.parseDouble(lonStr);
                Point p = new Point(lat, lon);
                points.add(p);
            }
        }catch (SAXException | IOException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        return points;
    }
}
