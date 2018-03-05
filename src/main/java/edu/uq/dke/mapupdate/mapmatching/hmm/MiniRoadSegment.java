package edu.uq.dke.mapupdate.mapmatching.hmm;

import traminer.util.spatial.objects.Segment;

import java.io.Serializable;
import java.util.Objects;

public class MiniRoadSegment implements Serializable {
    private Segment miniSegment;
    private String roadWayID;

    public MiniRoadSegment(Segment segment, String id) {
        this.miniSegment = segment;
        this.roadWayID = id;
    }

    public MiniRoadSegment(double x1, double y1, double x2, double y2, String id) {
        this.miniSegment = new Segment(x1, y1, x2, y2);
        this.roadWayID = id;
    }

    public Segment getMiniSegment() {
        return miniSegment;
    }

    public void setMiniSegment(Segment miniSegment) {
        this.miniSegment = miniSegment;
    }

    public String getRoadWayID() {
        return roadWayID;
    }

    public void setRoadWayID(String roadWayID) {
        this.roadWayID = roadWayID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(miniSegment, roadWayID);
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object obj) {
        MiniRoadSegment other = (MiniRoadSegment) obj;
        return (Objects.equals(miniSegment, other.miniSegment) && Objects.equals(roadWayID, other.roadWayID));
    }
}
