package pl.gpxplorer.processing;

import pl.gpxplorer.model.Point;
import pl.gpxplorer.model.PointsList;
import pl.gpxplorer.model.SegmentList;

import java.util.List;

public class SplitGPXByParts {
    public SegmentList split(PointsList pointList, int parts) {
        SegmentList segmentList = new SegmentList();
        int totalPoints = pointList.getLength();
        int baseSegments = totalPoints/parts;
        int extraSegments = totalPoints%parts;
        int start = 0;
        for (int i = 0; i < parts; i++) {
            int segmentCount = baseSegments + (i < extraSegments ? 1 : 0);
            int end = start + segmentCount;
            List<Point> points = pointList.getPoints().subList(start, end);
            PointsList pointsList = new PointsList();
            points.forEach(pointsList::add);
            segmentList.add(pointsList);
            start = end;
        }
        return segmentList;
    }

}

