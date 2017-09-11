package traminer.util.spatial.structures.voronoi;

import traminer.util.spatial.objects.Segment;

/**
 * A Voronoi Edge is composed by its end points X and
 * Y coordinates, and the two polygons (pivots) it
 * belongs to. Each Voronoi Edge is shared by 
 * exactly two polygons.
 * 
 * @author uqdalves
 */
@SuppressWarnings("serial")
public class VoronoiEdge extends Segment {
    /**
     * The Voronoi polygon at the left side of this edge
     */
    private final String leftSite;
    /**
     * The Voronoi polygon at the right side of this edge
     */
    private final String rightSite;

    /**
     * Creates a Voronoi edge with the given end-points.
     * 
     * @param x1 First end-point X coordinate.
     * @param y1 First end-point Y coordinate.
     * @param x2 Second end-point X coordinate.
     * @param y2 Second end-point Y coordinate.
     * @param leftSite The id of Voronoi polygon at 
     * the left side of this edge.
     * @param rightSite The id of Voronoi polygon at 
     * the right side of this edge.
     */
    public VoronoiEdge(double x1, double y1, double x2, double y2,
                       String leftSite, String rightSite) {
        super(x1, y1, x2, y2);
        this.leftSite = leftSite;
        this.rightSite = rightSite;
    }

    /**
     * @return The id of the Voronoi polygon at
     * the left side of this edge.
     */
    public String getLeftSite() {
        return leftSite;
    }

    /**
     * @return The id of the Voronoi polygon at
     * the right side of this edge.
     */
    public String getRightSite() {
        return rightSite;
    }

    @Override
    public String toString() {
        String s = super.toString() + ", " + leftSite + ", " + rightSite;
        return s;
    }

    @Override
    public void print() {
        System.out.println("VORONOI EDGE " + toString());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((leftSite == null) ? 0 : leftSite.hashCode());
        result = prime * result + ((rightSite == null) ? 0 : rightSite.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VoronoiEdge) {
            VoronoiEdge e = (VoronoiEdge) obj;
            if (this.x1() == e.x1() && this.x2() == e.x2() &&
                    this.y1() == e.y1() && this.y2() == e.y2())
                return true;
            if (this.x1() == e.x2() && this.x2() == e.x1() &&
                    this.y1() == e.y2() && this.y2() == e.y1())
                return true;
        }
        return false;
    }

    @Override
    public VoronoiEdge clone() {
        VoronoiEdge clone = new VoronoiEdge(x1(), y1(), x2(), y2(), leftSite, rightSite);
        super.cloneTo(clone);
        return clone;
    }
}
