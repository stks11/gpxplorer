package pl.gpxplorer.processing;

import pl.gpxplorer.model.Point;
import pl.gpxplorer.model.PointsList;
import pl.gpxplorer.model.SegmentList;

import java.util.ArrayList;
import java.util.List;

public class ElevationProfile {
    private static final double CLIMB_THRESHOLD_METERS = 3.0;
    private static final double SLOPE_WINDOW_METERS = 100.0;

    private final List<double[]> elevationSamples = new ArrayList<>();
    private final List<double[]> slopeSamples = new ArrayList<>();
    private double totalDistanceKm;
    private double ascent;
    private double descent;
    private double minElevation = Double.NaN;
    private double maxElevation = Double.NaN;

    public static ElevationProfile of(SegmentList segments) {
        ElevationProfile profile = new ElevationProfile();
        profile.compute(segments);
        return profile;
    }

    private void compute(SegmentList segments) {
        SplitGPXByKilometer distanceCalculator = new SplitGPXByKilometer();
        double distanceMeters = 0;
        for (PointsList segment : segments) {
            Point previous = null;
            for (Point point : segment) {
                if (previous != null) {
                    distanceMeters += distanceCalculator.haversine(previous, point);
                }
                previous = point;
                if (point.hasElevation()) {
                    elevationSamples.add(new double[]{distanceMeters / 1000.0, point.ele()});
                }
            }
        }
        totalDistanceKm = distanceMeters / 1000.0;
        if (elevationSamples.isEmpty()) {
            return;
        }
        computeClimbs();
        computeSlopes();
    }

    private void computeClimbs() {
        double reference = elevationSamples.getFirst()[1];
        minElevation = reference;
        maxElevation = reference;
        for (double[] sample : elevationSamples) {
            double elevation = sample[1];
            minElevation = Math.min(minElevation, elevation);
            maxElevation = Math.max(maxElevation, elevation);
            double diff = elevation - reference;
            if (diff >= CLIMB_THRESHOLD_METERS) {
                ascent += diff;
                reference = elevation;
            } else if (diff <= -CLIMB_THRESHOLD_METERS) {
                descent -= diff;
                reference = elevation;
            }
        }
    }

    private void computeSlopes() {
        double[] start = elevationSamples.getFirst();
        for (double[] sample : elevationSamples) {
            double distance = (sample[0] - start[0]) * 1000.0;
            if (distance >= SLOPE_WINDOW_METERS) {
                double slopePercent = (sample[1] - start[1]) / distance * 100.0;
                slopeSamples.add(new double[]{(start[0] + sample[0]) / 2, slopePercent});
                start = sample;
            }
        }
    }

    public boolean isEmpty() {
        return elevationSamples.isEmpty();
    }

    public List<double[]> getElevationSamples() {
        return elevationSamples;
    }

    public List<double[]> getSlopeSamples() {
        return slopeSamples;
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public double getAscent() {
        return ascent;
    }

    public double getDescent() {
        return descent;
    }

    public double getMinElevation() {
        return minElevation;
    }

    public double getMaxElevation() {
        return maxElevation;
    }
}
