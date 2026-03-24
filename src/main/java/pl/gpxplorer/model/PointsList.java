package pl.gpxplorer.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PointsList implements Iterable<Point> {
    private final List<Point> points;

    public PointsList() {
        points = new ArrayList<>();
    }

    public void add(Point point){
        points.add(point);
    }

    @Override
    public String toString() {
        return "PointsList{" +
                "points=" + points +
                '}';
    }

    public List<Point> getPoints() {
        return points;
    }

    public int getLength() {
        return points.size();
    }

    @Override
    public Iterator<Point> iterator() {
        return  points.iterator();
    }
    public void clear() {
        points.clear();
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }


}
