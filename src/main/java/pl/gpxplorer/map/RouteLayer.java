package pl.gpxplorer.map;

import com.gluonhq.maps.MapLayer;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;
import pl.gpxplorer.model.Point;
import pl.gpxplorer.model.PointsList;

import java.util.Random;
import java.util.function.Consumer;

public class RouteLayer extends MapLayer {

    private Color defaultColor = randomColor();
    private static final Color SELECTED_COLOR = Color.DODGERBLUE;
    private static final double ROUTE_WIDTH = 3;
    private static final double HITBOX_WIDTH = 12;

    private final PointsList route;
    private final Polyline polyline = new Polyline();
    private final Polyline hitbox = new Polyline();
    private boolean selected;
    private int orderIndex;

    public RouteLayer(PointsList route) {
        this.route = route;
        polyline.setStrokeWidth(ROUTE_WIDTH);
        polyline.setPickOnBounds(false);
        polyline.setMouseTransparent(true);
        updateStyle();

        hitbox.setStroke(Color.TRANSPARENT);
        hitbox.setStrokeWidth(HITBOX_WIDTH);
        hitbox.setPickOnBounds(false);
        hitbox.setCursor(Cursor.HAND);

        getChildren().addAll(hitbox, polyline);

    }

    public PointsList getRoute() {
        return route;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        updateStyle();
    }

    public void setOnRouteClicked(Consumer<RouteLayer> onRouteClicked) {
        hitbox.setOnMouseClicked((MouseEvent event) -> {
            onRouteClicked.accept(this);
            event.consume();
        });
    }

    @Override
    protected void initialize() {
        markDirty();
    }

    public void refresh() {
        markDirty();
    }

    private void updateStyle() {
        polyline.setStroke(selected ? SELECTED_COLOR : defaultColor);
    }

    @Override
    protected void layoutLayer() {
        polyline.getPoints().clear();
        hitbox.getPoints().clear();
        for (Point p : route) {
            Point2D pt = getMapPoint(p.lat(), p.lon());
            if (pt != null) {
                polyline.getPoints().addAll(
                        pt.getX(),
                        pt.getY()
                );
                hitbox.getPoints().addAll(
                        pt.getX(),
                        pt.getY()
                );
            }
        }
    }

    private Color randomColor(){
        Random random = new Random();
        int r =  random.nextInt(256);
        int g = random.nextInt(256);
        int b = random.nextInt(256);
        if (r == 30 && g == 144 && b == 255) {
            return randomColor();
        }
        return Color.rgb(r,g,b);
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;

    }

}
