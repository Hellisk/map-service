package traminer.util.trajectory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import traminer.util.DeltaEncoder;
import traminer.util.exceptions.SpatialObjectConstructionException;
import traminer.util.spatial.distance.EuclideanDistanceFunction;
import traminer.util.spatial.distance.PointDistanceFunction;
import traminer.util.spatial.objects.*;
import traminer.util.spatial.objects.st.STPoint;
import traminer.util.spatial.objects.st.STSegment;
import traminer.util.spatial.objects.st.SpatialTemporalObject;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A 2D spatial-temporal trajectory entity.
 * Discrete list of spatial-temporal points.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class Trajectory extends ComplexSpatialObject<STPoint> implements SpatialTemporalObject {
    // auxiliary LineString from JTS lib
    private com.vividsolutions.jts.geom.
            LineString JTSLineString = null;

    public Trajectory() {
        super(1);
    }

    public Trajectory(String id) {
        super(1);
        this.setId(id);
    }

    public Trajectory(List<STPoint> pointList) {
        super(pointList.size());
        this.addAll(pointList);
    }

    public Trajectory(STPoint... points) {
        super(points.length);
        for (STPoint p : points) {
            this.add(p);
        }
    }

    public Trajectory(String id, List<STPoint> pointList) {
        super(pointList.size());
        this.setId(id);
        this.addAll(pointList);
    }

    public Trajectory(String id, STPoint... points) {
        super(points.length);
        this.setId(id);
        for (STPoint p : points) {
            this.add(p);
        }
    }

    @Override
    public List<Point> getCoordinates() {
        List<Point> list = new ArrayList<Point>(size());
        for (Point p : this) {
            list.add(p);
        }
        return list;
    }

    public List<STPoint> getPoints() {
        return this;
    }

    @Override
    public List<Segment> getEdges() {
        List<Segment> list = new ArrayList<Segment>();
        Point pi, pj;
        for (int i = 0; i < size() - 1; i++) {
            pi = this.get(i);
            pj = this.get(i + 1);
            list.add(new Segment(pi.x(), pi.y(), pj.x(), pj.y()));
        }
        return list;
    }

    public List<STSegment> getSTEdges() {
        List<STSegment> list = new ArrayList<STSegment>();
        STPoint pi, pj;
        for (int i = 0; i < size() - 1; i++) {
            pi = this.get(i);
            pj = this.get(i + 1);
            list.add(new STSegment(pi, pj));
        }
        return list;
    }

    /**
     * Adds a spatial-temporal point to this trajectory.
     */
    public void add(double x, double y, long time) {
        this.add(new STPoint(x, y, time));
    }

    /**
     * Merges two trajectories.
     * Appends a trajectory t to the end of this trajectory.
     *
     * @return This merged trajectory.
     * @throws NullPointerException
     */
    public Trajectory add(Trajectory t) {
        if (t == null) {
            throw new NullPointerException("Trajectory "
                    + "for merge must not be null.");
        }
        this.addAll(t);
        return this;
    }

    /**
     * Return the initial time of this trajectory.
     * Time stamp of the first sample point.
     */
    @Override
    public long timeStart() {
        if (!this.isEmpty()) {
            return head().time();
        }
        return 0;
    }

    /**
     * Return the final time of this trajectory.
     * Time stamp of the last sample point.
     */
    @Override
    public long timeFinal() {
        if (!this.isEmpty()) {
            return tail().time();
        }
        return 0;
    }

    /**
     * Return the length of this trajectory.
     * Sum of the distances between every point.
     *
     * @param dist The point distance measure to use.
     */
    public double length(PointDistanceFunction dist) {
        double length = 0.0;
        if (!isEmpty()) {
            for (int i = 0; i < size() - 1; i++) {
                length += get(i).distance(get(i + 1), dist);
            }
        }
        return length;
    }

    /**
     * Return the time duration of this trajectory.
     * Time taken from the beginning to the end of the
     * trajectory.
     */
    public long duration() {
        if (!this.isEmpty()) {
            return (timeFinal() - timeStart());
        }
        return 0;
    }

    /**
     * Return the average speed of this trajectory
     * on a sphere surface (Earth).
     *
     * @param dist The point distance measure to use
     *             when calculating the speed.
     */
    public double speed(PointDistanceFunction dist) {
        if (!this.isEmpty() && duration() != 0) {
            return (length(dist) / duration());
        }
        return 0.0;
    }

    /**
     * Return the average sample rate of the points in
     * this trajectory (average time between every sample
     * point).
     */
    public double samplingRate() {
        if (!this.isEmpty()) {
            double rate = 0.0;
            STPoint pi, pj;
            for (int i = 0; i < this.size() - 1; i++) {
                pi = this.get(i);
                pj = this.get(i + 1);
                rate += pj.time() - pi.time();
            }
            return (rate / (this.size() - 1));
        }
        return 0.0;
    }

    /**
     * The 'head' of this trajectory: First sample point.
     */
    public STPoint head() {
        if (!this.isEmpty()) {
            return this.get(0);
        }
        return null;
    }

    /**
     * The 'tail' of this trajectory: Last sample point.
     */
    public STPoint tail() {
        if (!this.isEmpty()) {
            return this.get(this.size() - 1);
        }
        return null;
    }

    /**
     * Return a sub-trajectory of this trajectory, from
     * beginIndex inclusive to endIndex exclusive.
     * <p>
     * Note: trajectory index starts from 0 (zero).
     *
     * @throws IllegalArgumentException
     */
    public Trajectory subTrajectory(int beginIndex, int endIndex) {
        if (beginIndex < 0 || endIndex > size() || beginIndex > endIndex) {
            throw new IllegalArgumentException(
                    "Trajectory index out of bound.");
        }
        Trajectory sub = new Trajectory(this.getId());
        sub.addAll(this.subList(beginIndex, endIndex));
        return sub;
    }

    /**
     * Return the LineString object representation
     * of this Trajectory.
     */
    public LineString toLineString() {
        return new LineString(this);
    }

    /**
     * Get the Path2D (AWT) representation of this trajectory.
     *
     * @throws SpatialObjectConstructionException
     */
    public Path2D toPath2D() {
        if (!isEmpty()) {
            throw new SpatialObjectConstructionException(
                    "Semgments list for Path2D must not be empty.");
        }
        Path2D path = new Path2D.Double();
        path.moveTo(get(0).x(), get(0).y());
        for (int i = 1; i < size(); i++) {
            path.lineTo(get(i).x(), get(i).y());
        }

        return path;
    }

    @Override
    public Geometry toJTSGeometry() {
        if (JTSLineString == null) {
            Coordinate[] coords = new Coordinate[size()];
            int i = 0;
            for (Point p : this) {
                coords[i++] = new Coordinate(p.x(), p.y());
            }
            PackedCoordinateSequence points =
                    new PackedCoordinateSequence.Double(coords);

            JTSLineString = new com.vividsolutions.jts.geom.
                    LineString(points, new GeometryFactory());
        }
        return JTSLineString;
    }

    /**
     * The reverse representation of this Trajectory.
     */
    public Trajectory reverse() {
        Trajectory reverse = new Trajectory();
        for (int i = size() - 1; i >= 0; i--) {
            reverse.add(this.get(i).clone());
        }
        return reverse;
    }

    /**
     * Returns a copy of this Trajectory object
     * in Delta compression. Note that this method
     * only compress the spatial-temporal attributes.
     */
    public Trajectory deltaCompress() {
        double[] xValues = DeltaEncoder.deltaEncode(this.getXValues());
        double[] yValues = DeltaEncoder.deltaEncode(this.getYValues());
        long[] tValues = DeltaEncoder.deltaEncode(this.getTimeValues());

        Trajectory delta = new Trajectory();
        this.cloneTo(delta);
        delta.setSTCoordinates(xValues, yValues, tValues);

        return delta;
    }

    /**
     * Returns a copy of this Trajectory object after
     * Delta decompression. Note that this method
     * only decompress the spatial-temporal attributes.
     */
    public Trajectory deltaDecompress() {
        double[] xValues = DeltaEncoder.deltaDecode(this.getXValues());
        double[] yValues = DeltaEncoder.deltaDecode(this.getYValues());
        long[] tValues = DeltaEncoder.deltaDecode(this.getTimeValues());

        Trajectory delta = new Trajectory();
        delta.setSTCoordinates(xValues, yValues, tValues);
        this.cloneTo(delta);

        return delta;
    }

    @Override
    public boolean isClosed() {
        if (this == null || size() < 2) return false;
        return get(0).equals2D(get(size() - 1));
    }

    /**
     * Set the spatial-temporal coordinate
     * attributes of this trajectory.
     */
    public void setSTCoordinates(
            double[] xValues,
            double[] yValues,
            long[] timeValues) {
        if (xValues == null || yValues == null ||
                timeValues == null) {
            throw new NullPointerException("Coordinates attributes "
                    + "arrays must not be null.");
        }
        if (xValues.length != yValues.length ||
                xValues.length != timeValues.length) {
            throw new IllegalArgumentException("Coordinates attributes "
                    + "arrays must be of equal size.");
        }
        this.clear();
        for (int i = 0; i < xValues.length; i++) {
            add(xValues[i],
                    yValues[i],
                    timeValues[i]);
        }
    }

    /**
     * Returns an array with the X coordinates of
     * this trajectory sample points.
     */
    public double[] getXValues() {
        double[] result = new double[this.size()];
        int i = 0;
        for (STPoint p : this) {
            result[i++] = p.x();
        }
        return result;
    }

    /**
     * Returns an array with the Y coordinates of
     * this trajectory sample points.
     */
    public double[] getYValues() {
        double[] result = new double[this.size()];
        int i = 0;
        for (STPoint p : this) {
            result[i++] = p.y();
        }
        return result;
    }

    /**
     * Returns an array with the TIME coordinates of
     * this trajectory sample points.
     */
    public long[] getTimeValues() {
        long[] result = new long[this.size()];
        int i = 0;
        for (STPoint p : this) {
            result[i++] = p.time();
        }
        return result;
    }

    @Override
    public boolean equals2D(SpatialObject obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (obj instanceof Trajectory) {
            Trajectory t = (Trajectory) obj;
            if (this.size() != t.size()) {
                return false;
            }
            for (int i = 0; i < size(); i++) {
                if (!t.get(i).equals2D(this.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean equalsST(SpatialTemporalObject obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (obj instanceof Trajectory) {
            Trajectory t = (Trajectory) obj;
            if (t.timeStart() != this.timeStart() ||
                    t.timeFinal() != this.timeFinal()) return false;
            return this.equals2D(t);
        }
        return false;
    }

    @Override
    public Trajectory clone() {
        Trajectory clone = new Trajectory();
        for (STPoint p : this) {
            clone.add(p.clone());
        }
        super.cloneTo(clone);
        return clone;
    }

    @Override
    public String toString() {
        String s = this.getId() + " ( ";
        for (STPoint p : this) {
            s += ", " + p.toString();
        }
        s = s.replaceFirst(",", "");
        return s + " )";
    }

    @Override
    public void display() {
        if (this.isEmpty()) return;

        Graph graph = new SingleGraph("Trajectory");
        graph.display(false);
        // create one node per trajectory point
        graph.addNode("N0");
        for (int i = 1; i < this.size(); i++) {
            graph.addNode("N" + i);
            graph.addEdge("E" + (i - 1) + "-" + i, "N" + (i - 1), "N" + i);
        }
        for (int i = 0; i < this.size(); i++) {
            Point p = this.get(i);
            graph.getNode("N" + i).setAttribute("xyz", p.x(), p.y(), 0);
        }
        graph.addNode("A0");
        graph.addNode("A1");
        graph.addNode("A2");
    }

    @Override
    public void print() {
        System.out.println("TRAJECTORY ( " + toString() + " )");
    }

    /**
     * Compare trajectories by LENGTH in ascending order.
     * Euclidean distance comparator only.
     */
    public static final Comparator<Trajectory> LENGTH_COMPARATOR =
            new Comparator<Trajectory>() {
                PointDistanceFunction dist = new EuclideanDistanceFunction();

                @Override
                public int compare(Trajectory o1, Trajectory o2) {
                    if (o1.length(dist) < o2.length(dist))
                        return -1;
                    if (o1.length(dist) > o2.length(dist))
                        return 1;
                    return 0;
                }
            };

    /**
     * Compare trajectories by time DURATION in ascending order.
     */
    public static final Comparator<Trajectory> DURATION_COMPARATOR =
            new Comparator<Trajectory>() {
                @Override
                public int compare(Trajectory o1, Trajectory o2) {
                    if (o1.duration() < o2.duration())
                        return -1;
                    if (o1.duration() > o2.duration())
                        return 1;
                    return 0;
                }
            };

    /**
     * Compare trajectories by SPEED in ascending order.
     * Euclidean space only.
     */
    public static final Comparator<Trajectory> SPEED_COMPARATOR =
            new Comparator<Trajectory>() {
                PointDistanceFunction dist = new EuclideanDistanceFunction();

                @Override
                public int compare(Trajectory o1, Trajectory o2) {
                    if (o1.speed(dist) < o2.speed(dist))
                        return -1;
                    if (o1.speed(dist) > o2.speed(dist))
                        return 1;
                    return 0;
                }
            };

}
