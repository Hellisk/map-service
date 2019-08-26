package algorithm.mapmatching.simpleHMM;

import util.object.spatialobject.Point;

public class StateSample {

    private Point sampleMeasurement;
    private double heading;
    private double sampleTime = -1;


    public StateSample(Point point, double heading, double time) {
        this.sampleMeasurement = point;
        this.heading = heading;
        this.sampleTime = time;
    }

    public double x() {
        return this.sampleMeasurement.x();
    }

    public double y() {
        return this.sampleMeasurement.y();
    }

    public Point getSampleMeasurement() {
        return sampleMeasurement;
    }

    public double getTime() {
        return sampleTime;
    }

    public double getHeading() {
        return heading;
    }
}
