package pl.gpxplorer.processing;

import pl.gpxplorer.model.Point;
import pl.gpxplorer.model.PointsList;
import pl.gpxplorer.model.SegmentList;

public class SplitGPXByKilometer {
    public double haversine(Point p1, Point p2) {

        double R = 6371000;

        double lat1 = Math.toRadians(p1.lat());
        double lat2 = Math.toRadians(p2.lat());
        double dLat = Math.toRadians(p2.lat() - p1.lat());
        double dLon = Math.toRadians(p2.lon() - p1.lon());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    public double distance (PointsList points){
        int start = 0;
        int end = 1;
        double result = 0;

        for (int i = 0; i < points.getLength(); i++) {
            Point p1 = points.getPoints().get(start);
            Point p2 = points.getPoints().get(end);
            result += haversine(p1,p2);
            start++;
            end++;
            if (start == points.getLength() || end == points.getLength()) {
                break;
            }
        }
        return result;
    }

    public SegmentList split(PointsList points, double distanceMeter) {
        double distanceM = 0;
        PointsList list = new PointsList();
        SegmentList segmentList = new SegmentList();
        if (points.getLength() == 0) {
            return segmentList;
        }
        list.add(points.getPoints().getFirst());
        for (int i = 1; i < points.getLength(); i++) {
            Point prev = points.getPoints().get(i - 1);
            Point curr = points.getPoints().get(i);

            double remainingEdgeDistance = haversine(prev, curr);
            Point currentStart = prev;

            if (distanceM + remainingEdgeDistance < distanceMeter) {
                list.add(curr);
                distanceM += remainingEdgeDistance;
            } else {
                while (distanceM + remainingEdgeDistance >= distanceMeter) {
                    double distanceDiff = distanceMeter - distanceM;
                    Point splitPoint = createPoint(currentStart, curr, distanceDiff, remainingEdgeDistance);
                    list.add(splitPoint);
                    segmentList.add(list);
                    list = new PointsList();
                    list.add(splitPoint);
                    remainingEdgeDistance -= distanceDiff;
                    currentStart = splitPoint;
                    distanceM = 0;
                }
                list.add(curr);
                distanceM += remainingEdgeDistance;
            }
        }

        if (!list.isEmpty()) {
            segmentList.add(list);
        }

        return segmentList;
    }

    public Point createPoint(Point p1, Point p2, double meterDiff, double meters) {
        double ratio = meterDiff/meters;
        double lat = p1.lat() + ratio * (p2.lat() - p1.lat());
        double lon =  p1.lon() + ratio * (p2.lon() - p1.lon());

        return new Point(lat,lon);
    }
}
