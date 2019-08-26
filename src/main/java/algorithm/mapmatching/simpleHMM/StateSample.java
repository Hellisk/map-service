package algorithm.mapmatching.simpleHMM;

import util.object.spatialobject.Point;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StateSample that = (StateSample) o;

        return sampleTime == that.getTime()
                && heading == that.getHeading()
                && sampleMeasurement.equals(that.getSampleMeasurement());
    }

    @Override
    public int hashCode() {
        return Objects.hash(sampleMeasurement, sampleTime, heading);
    }
}
