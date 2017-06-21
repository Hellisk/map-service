package traminer.util.spatial.objects;

import traminer.util.spatial.SpatialInterface;

/**
 * Implements a generic immutable 2D vector entity.
 * Vectors starting from origin (0,0)-->(vx,vy).
 *
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class Vector2D
        extends org.apache.commons.math3.geometry.euclidean.twod.Vector2D
        implements SpatialInterface {

    /**
     * Vector coordinates.
     */
    private final double vx, vy;

    /**
     * Constructs an empty vector
     */
    public Vector2D() {
        super(0.0, 0.0);
        this.vx = 0.0;
        this.vy = 0.0;
    }

    /**
     * Vectors starting from origin (0,0) to (vx,vy).
     */
    public Vector2D(double vx, double vy) {
        super(vx, vy);
        this.vx = vx;
        this.vy = vy;
    }

    /**
     * The Norm (length) of this vector.
     */
    public double norm() {
        return Math.sqrt((vx * vx) + (vy * vy));
    }

    /**
     * Dot product between this vector and the given vector U.
     */
    public double dotProduct(Vector2D u) {
        return dotProduct(u.vx, u.vy);
    }

    /**
     * Dot product between this vector and the given vector U
     * given by x and y coordinates.
     */
    public double dotProduct(double ux, double uy) {
        double dot_product = (this.vx * ux) + (this.vy * uy);
        return dot_product;
    }

    /**
     * Dot product between two generic vectors V * U.
     */
    public static double dotProduct(
            double vx, double vy, double ux, double uy) {
        double dot_product = (vx * ux) + (vy * uy);
        return dot_product;
    }

    /**
     * Cross product between this vector and the given vector U.
     */
    public double crossProduct(Vector2D u) {
        return crossProduct(u.vx, u.vy);
    }

    /**
     * Cross product between this vector and the given vector U
     * given by x and y coordinates.
     */
    public double crossProduct(double ux, double uy) {
        double cross = (this.vx * uy) - (ux * this.vy);
        return cross;
    }

    /**
     * Compute the cross product of two generic vectors V and U.
     */
    public static double crossProduct(
            double vx, double vy, double ux, double uy) {
        double cross = (vx * uy) - (ux * vy);
        return cross;
    }

    /**
     * The intern angle between this vector and the vector U.
     */
    public double angle(Vector2D u) {
        return angle(u.vx, u.vy);
    }

    /**
     * The intern angle between this vector and the vector U
     * given by x and y coordinates.
     */
    public double angle(double ux, double uy) {
        // cosine of the angle
        double cos = (ux * this.vx) + (uy * this.vy);
        cos = cos / (Math.sqrt((ux * ux) + (uy * uy)) *
                Math.sqrt((this.vx * this.vx) + (this.vy * this.vy)));
        return Math.acos(cos);
    }

    /**
     * The intern angle angle between two 2D generic vectors U and V.
     */
    public static double angleBetweenVectors(
            double ux, double uy, double vx, double vy) {
        // cosine of the angle
        double cos = (ux * vx) + (uy * vy);
        cos = cos / (Math.sqrt((ux * ux) + (uy * uy)) *
                Math.sqrt((vx * vx) + (vy * vy)));
        return Math.acos(cos);
    }

    /**
     * The intern angle between this vector and the X axis.
     */
    public double angleX() {
        return angle(1.0, 0.0);
    }

    /**
     * The intern angle between this vector and the Y axis.
     */
    public double angleY() {
        return angle(0.0, 1.0);
    }

    /**
     * The angle between two generic vectors V and U.
     */
    public static double angle(
            double vx, double vy, double ux, double uy) {
        // cosine of the angle
        double cos = (ux * vx) + (uy * vy);
        cos = cos / (Math.sqrt((ux * ux) + (uy * uy)) *
                Math.sqrt((vx * vx) + (vy * vy)));
        return Math.acos(cos);
    }

    @Override
    public String toString() {
        String s = String.format("<%.3f, %.3f>", vx, vy);
        return s;
    }

    public void print() {
        System.out.println("VECTOR2D " + toString());
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        long temp;
        temp = Double.doubleToLongBits(vx);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(vy);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    public boolean equals2D(Vector2D obj) {
        return (obj.vx == vx && obj.vy == vy);
    }

    @Override
    public Vector2D clone() {
        return new Vector2D(vx, vy);
    }
}
