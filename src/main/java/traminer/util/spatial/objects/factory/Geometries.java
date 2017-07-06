package traminer.util.spatial.objects.factory;

import traminer.util.spatial.objects.*;
import traminer.util.spatial.objects.st.STCircle;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.spatial.objects.st.STRectangle;
import traminer.util.spatial.objects.st.STSegment;
import traminer.util.trajectory.Trajectory;

import java.io.Serializable;
import java.util.List;

/**
 * Spatial objects factory.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public final class Geometries implements Serializable {

    public static final Point point(double x, double y) {
        return new Point(x, y);
    }

    public static final PointND pointND(double[] coordinates) {
        return new PointND(coordinates);
    }

    public static final STPoint stPoint(double x, double y, long time) {
        return new STPoint(x, y, time);
    }

    public static final Segment segment(double x1, double y1, double x2, double y2) {
        return new Segment(x1, y1, x2, y2);
    }

    public static final Rectangle rectangle(double min_x, double min_y, double max_x, double max_y) {
        return new Rectangle(min_x, min_y, max_x, max_y);
    }

    public static final STRectangle stRectangle(double min_x, double min_y,
                                                double max_x, double max_y, long t_ini, long t_end) {
        return new STRectangle(min_x, min_y, max_x, max_y, t_ini, t_end);
    }

    public static final STSegment stSegment(double x1, double y1, long t1,
                                            double x2, double y2, long t2) {
        return new STSegment(x1, y1, t1, x2, y2, t2);
    }

    public static final Circle circle(double center_x, double center_y, double radius) {
        return new Circle(center_x, center_y, radius);
    }

    public static final STCircle stCircle(double center_x, double center_y,
                                          double radius, long t_ini, long t_end) {
        return new STCircle(center_x, center_y, radius, t_ini, t_end);
    }

    public static final LineString lineString(List<? extends Point> pointList) {
        return new LineString(pointList);
    }

    public static final LineString lineString(Point... points) {
        return new LineString(points);
    }

    public static final Polygon polygon(List<? extends Point> vertexList) {
        return new Polygon(vertexList);
    }

    public static final Polygon polygon(Point... vertexes) {
        return new Polygon(vertexes);
    }

    public static final Triangle triangle(double x1, double y1, double x2,
                                          double y2, double x3, double y3) {
        return new Triangle(x1, y1, x2, y2, x3, y3);
    }

    public static final Vector2D vector2D(double vx, double vy) {
        return new Vector2D(vx, vy);
    }

    public static final Trajectory trajectory(List<STPoint> pointList) {
        return new Trajectory(pointList);
    }

    public static final Trajectory trajectory(STPoint... points) {
        return new Trajectory(points);
    }
}
