package mapupdate.util.object.datastructure;

import mapupdate.util.object.spatialobject.Point;
import mapupdate.util.object.spatialobject.Segment;

public class PointMatch {
    private Point matchPoint;
    private Segment matchedSegment;
    private String roadID;

    public PointMatch() {
        this.matchPoint = new Point();
        this.matchedSegment = new Segment();
        this.roadID = "";
    }

    public PointMatch(Point matchingPoint, Segment matchedSegment, String roadID) {
        this.matchPoint = new Point(matchingPoint.x(), matchingPoint.y());
        this.matchedSegment = matchedSegment;
        this.roadID = roadID;
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
        return roadID;
    }

    public void setRoadID(String roadID) {
        this.roadID = roadID;
    }

    public double lon() {
        return this.matchPoint.x();
    }

    public double lat() {
        return this.matchPoint.y();
    }

    public PointMatch clone() {
        return new PointMatch(this.matchPoint, this.matchedSegment, this.roadID);
    }

}
