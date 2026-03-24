package pl.gpxplorer.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SegmentList implements Iterable<PointsList> {
    private List<PointsList> segmentList;

    public SegmentList() {
        this.segmentList = new ArrayList<>();
    }

    public void add(PointsList points) {
        segmentList.add(points);
    }

    public int getSize(){
        return segmentList.size();
    }

    public List<PointsList> getSegmentList() {
        return segmentList;
    }

    @Override
    public String toString() {
        return "SegmentList{" +
                "segmentList=" + segmentList +
                '}';
    }

    @Override
    public Iterator<PointsList> iterator() {
        return segmentList.iterator();
    }
}
