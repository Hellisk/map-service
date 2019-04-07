package mapupdate.util.object.datastructure;

import mapupdate.util.object.spatialobject.Point;
import mapupdate.util.object.spatialobject.Segment;

public class PointMatch extends LengthBasedItem {
    private Point matchPoint;
    private Segment matchedSegment;

    public PointMatch() {
        super();
        this.matchPoint = new Point();
        this.matchedSegment = new Segment();
    }

    public PointMatch(Point matchingPoint, Segment matchedSegment, String roadID) {
        super(matchedSegment.length(), roadID);
        this.matchPoint = new Point(matchingPoint.x(), matchingPoint.y());
        this.matchedSegment = matchedSegment;
    }

    public Point getMatchPoint() {
        return matchPoint;
    }

    public void setMatchPoint(Point matchPoint) {
        this.matchPoint = matchPoint;
    }

    public Segment getMatchedSegment() {
        return matchedSegment;
    }

    public void setMatchedSegment(Segment matchedSegment) {
        this.matchedSegment = matchedSegment;
    }

    public String getRoadID() {
        return super.getID();
    }

    public void setRoadID(String roadID) {
        super.setID(roadID);
    }

    public double lon() {
        return this.matchPoint.x();
    }

    public double lat() {
        return this.matchPoint.y();
    }

    public PointMatch clone() {
        return new PointMatch(this.matchPoint, this.matchedSegment, this.getRoadID());
    }

    @Override
    public String toString() {
        return matchPoint.x() + "_" + matchPoint.y() + "," + matchedSegment.x1() + "_" + matchedSegment.y1() + "," + matchedSegment.x2() + "_" + matchedSegment.y2() + "," + super.getID();
    }
}
