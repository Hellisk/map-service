package traminer.util.spatial.objects;

import traminer.util.spatial.SpatialInterface;

/**
 * Implements a generic immutable 2D vector entity.
 * <br> Vectors starting from origin (0,0) to (vx,vy).
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
     * Creates a new 2D vector in the origin (0,0).
     */
    public Vector2D() {
        super(0.0, 0.0);
        this.vx = 0.0;
        this.vy = 0.0;
    }

    /**
     * Creates a new 2D vector, from the origin (0,0)
     * to the given (vx,vy) coordinates.
     *
     * @param vx Vector X coordinate.
     * @param vy Vector Y coordinate.
     */
    public Vector2D(double vx, double vy) {
        super(vx, vy);
        this.vx = vx;
        this.vy = vy;
    }

    /**
     * @return The Norm (length) of this vector.
     */
    public double norm() {
        return Math.sqrt((vx * vx) + (vy * vy));
    }

    /**
     * Get the dot product between this vector and the given vector V.
     *
     * @param v
     * @return Dot product between this and V.
     */
    public double dotProduct(Vector2D v) {
        return dotProduct(v.vx, v.vy);
    }

    /**
     * Dot product between this vector and the given vector V
     * given by x and y coordinates.
     *
     * @param vx
     * @param vy
     * @return The dot product between this and (vx, vy).
     */
    public double dotProduct(double vx, double vy) {
        double dotProduct = (this.vx * vx) + (this.vy * vy);
        return dotProduct;
    }

    /**
     * Compute the dot product between two 2D vectors V * U.
     *
     * @param vx
     * @param vy
     * @param ux
     * @param uy
     * @return The dot product V * U.
     */
    public static double dotProduct(
            double vx, double vy, double ux, double uy) {
        double dotProduct = (vx * ux) + (vy * uy);
        return dotProduct;
    }

    /**
     * Get the cross product between this vector and the given vector V.
     *
     * @param v
     * @return The cross product between this and V.
     */
    public double crossProduct(Vector2D v) {
        return crossProduct(v.vx, v.vy);
    }

    /**
     * Get the cross product between this vector and the given vector V
     * given by x and y coordinates.
     *
     * @param vx
     * @param vy
     * @return The cross product between this and (vx, vy).
     */
    public double crossProduct(double vx, double vy) {
        double cross = (this.vx * vy) - (vx * this.vy);
        return cross;
    }

    /**
     * Compute the cross product between two 2D vectors V x U.
     *
     * @param vx
     * @param vy
     * @param ux
     * @param uy
     * @return The cross product V x U.
     */
    public static double crossProduct(
            double vx, double vy, double ux, double uy) {
        double cross = (vx * uy) - (ux * vy);
        return cross;
    }

    /**
     * Get the intern angle between this vector and the 2D vector V.
     *
     * @param v
     * @return The intern angle between this and V.
     */
    public double angle(Vector2D v) {
        return angle(v.vx, v.vy);
    }

    /**
     * Get the intern angle between this vector and the 2D vector V
     * given by x and y coordinates.
     *
     * @param vx
     * @param vy
     * @return The intern angle between this and V.
     */
    public double angle(double vx, double vy) {
        // cosine of the angle
        double cos = (vx * this.vx) + (vy * this.vy);
        cos = cos / (Math.sqrt((vx * vx) + (vy * vy)) *
                Math.sqrt((this.vx * this.vx) + (this.vy * this.vy)));
        return Math.acos(cos);
    }

    /**
     * Computes the intern angle angle between two 2D vectors U and V.
     *
     * @param ux
     * @param uy
     * @param vx
     * @param vy
     * @return The intern angle between U and V.
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
     * @return The intern angle between this 2D vector and the X axis.
     */
    public double angleX() {
        return angle(1.0, 0.0);
    }

    /**
     * @return The intern angle between this 2D vector and the Y axis.
     */
    public double angleY() {
        return angle(0.0, 1.0);
    }

    @Override
    public String toString() {
        String s = String.format("<%.5f, %.5f>", vx, vy);
        return s;
    }

    /**
     * Print this Vector to the system output.
     */
    public void print() {
        println("VECTOR2D " + toString());
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

    /**
     * Check whether these two vectors have the same
     * spatial coordinates.
     *
     * @param obj The 2D vector to compare.
     * @return True if these two vectors have the
     * same spatial coordinates.
     */
    public boolean equals2D(Vector2D obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        return (obj.vx == vx && obj.vy == vy);
    }

    @Override
    public Vector2D clone() {
        return new Vector2D(vx, vy);
    }
}
