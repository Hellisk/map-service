package edu.uq.dke.mapupdate.datatype;

import traminer.util.spatial.objects.Point;

public class PointProjection {
    private Point p;
    private double fractionAlong;
    private double cprojLength;

    public PointProjection(Point p, double fractionAlong, double cprojLength) {
        this.p = p;
        this.fractionAlong = fractionAlong;
        this.cprojLength = cprojLength;
    }

    public Point getP() {
        return p;
    }

    public void setP(Point p) {
        this.p = p;
    }

    public double getFractionAlong() {
        return fractionAlong;
    }

    public void setFractionAlong(double fractionAlong) {
        this.fractionAlong = fractionAlong;
    }

    public double getCprojLength() {
        return cprojLength;
    }

    public void setCprojLength(double cprojLength) {
        this.cprojLength = cprojLength;
    }
}
