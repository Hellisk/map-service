package util.object.structure;

import org.apache.log4j.Logger;
import util.function.DistanceFunction;
import util.object.spatialobject.Point;
import util.object.spatialobject.Segment;

import java.util.Objects;

/**
 * The matching result of a trajectory point.
 */
public class PointMatch extends LengthObject {
	
	private static final Logger LOG = Logger.getLogger(PointMatch.class);
	
	private Point matchPoint;
	private Segment matchedSegment;
	
	public PointMatch(DistanceFunction df) {
		super(df);
		this.matchPoint = new Point(df);
		this.matchedSegment = new Segment(df);
	}
	
	public PointMatch(Point matchingPoint, Segment matchedSegment, String roadID) {
		super(matchedSegment.length(), roadID, matchingPoint.getDistanceFunction());
		if (!matchingPoint.getDistanceFunction().getClass().getName().equals(matchedSegment.getDistanceFunction().getClass().getName()))
			LOG.error("The matching point and segment are from different coordination system.");
		this.matchPoint = matchingPoint;
		this.matchedSegment = matchedSegment;
	}
	
	public static PointMatch parsePointMatch(String s, DistanceFunction df) {
		String[] matchInfo = s.split(" ");
		if (matchInfo.length != 7)
			throw new IllegalArgumentException("The input text cannot be parsed to a PointMatch: " + s);
		Point currPoint = new Point(Double.parseDouble(matchInfo[0]), Double.parseDouble(matchInfo[1]), df);
		Segment currMatchSegment = new Segment(Double.parseDouble(matchInfo[2]), Double.parseDouble(matchInfo[3]),
				Double.parseDouble(matchInfo[4]), Double.parseDouble(matchInfo[5]), df);
		String id = matchInfo[6].equals("null") ? "" : matchInfo[6];
		return new PointMatch(currPoint, currMatchSegment, id);
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
		super.setID(matchedSegment.getID());
		super.setLength(matchedSegment.length());
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
		return matchPoint.x() + " " + matchPoint.y() + " " + matchedSegment.x1() + " " + matchedSegment.y1() + " " + matchedSegment.x2()
				+ " " + matchedSegment.y2() + " " + (super.getID().equals("") ? "null" : super.getID());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PointMatch that = (PointMatch) o;
		return matchPoint.equals(that.matchPoint) &&
				matchedSegment.equals(that.matchedSegment) &&
				super.getID().equals(that.getRoadID());
	}

	@Override
	public int hashCode() {
		return Objects.hash(matchPoint, matchedSegment);
	}
}