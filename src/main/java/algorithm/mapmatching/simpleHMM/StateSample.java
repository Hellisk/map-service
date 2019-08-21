package algorithm.mapmatching.simpleHMM;

import util.object.spatialobject.Point;

public class StateSample {

    private Point sampleMeasurement;
    private long sampleTime;
    private double heading;


    public StateSample(Point point, long time) {
        sampleMeasurement = point;
        sampleTime = time;
    }

    public double[] geometry() {
        return new double[]{sampleMeasurement.x(), sampleMeasurement.y()};
    }

    public Point getSampleMeasurement() {
        return sampleMeasurement;
    }

    public long getSampleTime() {
        return sampleTime;
    }

    public double getHeading() {
        return heading;
    }

    public void setHeading(double heading) {
        this.heading = heading;
    }
}
